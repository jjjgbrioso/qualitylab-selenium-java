package com.qualitylab.reporting;

import com.qualitylab.config.Config;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.openqa.selenium.Pdf;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.print.PageMargin;
import org.openqa.selenium.print.PageSize;
import org.openqa.selenium.print.PrintOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes a one-page HTML summary of every run, with a "Download PDF" button, and the PDF itself:
 *
 * <pre>
 *   reports/summary/test-report.html
 *   reports/summary/test-report.pdf
 *   reports/history/&lt;date-time&gt;/    a copy of both, kept when target/ is cleaned
 * </pre>
 *
 * The PDF is printed by headless Chrome through WebDriver, so no extra library is needed. When the
 * run ends the report opens in Chrome; OPEN_REPORT (always, on-failure, never) changes that.
 */
public class SummaryReportPlugin implements ConcurrentEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(SummaryReportPlugin.class);
    private static final Path SUMMARY = Paths.get("reports", "summary");
    private static final Path HISTORY = Paths.get("reports", "history");
    private static final String TITLE = "QualityLab test report (Selenium Java)";

    private record Entry(UUID id, String featureFile, int line, String name, Status status, Duration duration, String error) {}

    private final List<Entry> entries = new ArrayList<>();
    private final Map<UUID, byte[]> screenshots = new ConcurrentHashMap<>();
    private Instant started = Instant.now();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, event -> started = event.getInstant());
        publisher.registerHandlerFor(EmbedEvent.class, this::onEmbed);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onScenarioFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    private void onEmbed(EmbedEvent event) {
        if ("image/png".equals(event.getMediaType())) {
            screenshots.put(event.getTestCase().getId(), event.getData());
        }
    }

    private synchronized void onScenarioFinished(TestCaseFinished event) {
        var testCase = event.getTestCase();
        var result = event.getResult();
        String error = result.getError() == null ? "" : shorten(String.valueOf(result.getError().getMessage()));
        String file = testCase.getUri().getPath().replaceAll(".*/", "");
        entries.add(new Entry(testCase.getId(), file, testCase.getLocation().getLine(), testCase.getName(),
                result.getStatus(), result.getDuration(), error));
    }

    private synchronized void onRunFinished(TestRunFinished event) {
        if (entries.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(SUMMARY);
            Path html = SUMMARY.resolve("test-report.html");
            Path pdf = SUMMARY.resolve("test-report.pdf");
            Duration duration = Duration.between(started, event.getInstant());

            // First the page the PDF is printed from, then the final page with the PDF in its button.
            Files.writeString(html, render(duration, null, null), StandardCharsets.UTF_8);
            String pdfError = null;
            try {
                printPdf(html, pdf);
                String href = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(pdf));
                Files.writeString(html, render(duration, href, null), StandardCharsets.UTF_8);
            } catch (RuntimeException | IOException error) {
                pdfError = firstLine(String.valueOf(error.getMessage()));
                Files.writeString(html, render(duration, null, pdfError), StandardCharsets.UTF_8);
            }

            Path history = HISTORY.resolve(stamp(started));
            Files.createDirectories(history);
            Files.copy(html, history.resolve("test-report.html"), StandardCopyOption.REPLACE_EXISTING);
            if (pdfError == null) {
                Files.copy(pdf, history.resolve("test-report.pdf"), StandardCopyOption.REPLACE_EXISTING);
            }
            LOG.info("Summary report: {}{}", html, pdfError == null ? " and " + pdf : " (PDF not created: " + pdfError + ")");

            if (shouldOpen()) {
                openInChrome(html.toAbsolutePath());
            }
        } catch (IOException error) {
            LOG.warn("Could not write the summary report: {}", error.getMessage());
        }
    }

    // ----- Rendering -----

    private String render(Duration duration, String pdfHref, String pdfError) {
        List<Entry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparing(Entry::featureFile).thenComparingInt(Entry::line));
        Map<String, List<Entry>> byModule = new LinkedHashMap<>();
        for (Entry entry : sorted) {
            byModule.computeIfAbsent(module(entry.featureFile()), key -> new ArrayList<>()).add(entry);
        }

        int total = sorted.size();
        long passed = sorted.stream().filter(e -> outcome(e.status()).equals("passed")).count();
        long failed = sorted.stream().filter(e -> outcome(e.status()).equals("failed")).count();
        long skipped = sorted.stream().filter(e -> outcome(e.status()).equals("skipped")).count();
        long ran = total - skipped;
        double passRate = ran == 0 ? 0 : Math.round(passed * 1000.0 / ran) / 10.0;
        String runStatus = failed == 0 ? "passed" : "failed";
        String browser = browserName(Config.browser()) + (Config.headless() ? " (headless)" : "");

        String button;
        if (pdfError != null) {
            button = "<span class=\"button disabled\" title=\"" + escape(pdfError) + "\">PDF not available</span>";
        } else if (pdfHref != null) {
            // The PDF is embedded in the link: Chrome ignores "download" on links to local files, but
            // always saves an embedded file.
            button = "<a class=\"button\" href=\"" + pdfHref + "\" download=\"qualitylab-selenium-report-" + stamp(started)
                    + ".pdf\">Download PDF</a>";
        } else {
            button = "<span class=\"button disabled\">Preparing PDF</span>";
        }

        StringBuilder html = new StringBuilder(64_000);
        html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>").append(escape(TITLE)).append("</title>\n<style>").append(CSS).append("</style>\n</head>\n<body>\n<main>\n");

        html.append("<header><div><h1>").append(escape(TITLE)).append("</h1><div class=\"meta\"><b>")
                .append(escape(displayDate(started))).append("</b> &middot; ").append(escape(Config.environment().toUpperCase(Locale.ROOT)))
                .append(" &middot; ").append(escape(Config.baseUrl())).append("<br>")
                .append(plural(total, "scenario")).append(" &middot; Browser: ").append(escape(browser))
                .append(" &middot; Duration: ").append(escape(formatDuration(duration)))
                .append("</div><span class=\"status ").append(runStatus).append("\">Run ").append(runStatus).append("</span></div>")
                .append("<div class=\"actions\">").append(button)
                .append("<a class=\"button secondary\" href=\"../../target/cucumber-reports/cucumber.html\">Full Cucumber report</a>")
                .append("</div></header>\n");

        html.append("<div class=\"tiles\">")
                .append(tile("", total, "Scenarios"))
                .append(tile("passed", passed, "Passed"))
                .append(tile("failed", failed, "Failed"))
                .append(tile("", skipped, "Skipped"))
                .append("<div class=\"tile\"><div class=\"value\">").append(formatRate(passRate)).append("%</div><div class=\"label\">Pass rate</div></div>")
                .append("</div>\n");

        List<Entry> failures = sorted.stream().filter(e -> outcome(e.status()).equals("failed")).toList();
        if (!failures.isEmpty()) {
            html.append("<section class=\"failures\"><h2>Failures <span class=\"muted\">").append(failures.size()).append("</span></h2>");
            for (Entry failure : failures) {
                html.append("<article class=\"failure\"><h3>").append(escape(failure.name()))
                        .append(" <span class=\"muted\">").append(escape(failure.featureFile() + ":" + failure.line())).append("</span></h3>")
                        .append("<pre>").append(escape(failure.error().isBlank() ? "Status: " + failure.status() : failure.error())).append("</pre>");
                byte[] png = screenshots.get(failure.id());
                if (png != null) {
                    html.append("<img src=\"data:image/png;base64,").append(Base64.getEncoder().encodeToString(png))
                            .append("\" alt=\"Screenshot at the moment of failure\">");
                }
                html.append("</article>");
            }
            html.append("</section>\n");
        }

        for (Map.Entry<String, List<Entry>> module : byModule.entrySet()) {
            html.append("<section class=\"module\"><h2>").append(escape(module.getKey())).append(" <span class=\"muted\">")
                    .append(plural(module.getValue().size(), "scenario")).append("</span></h2><div class=\"table-wrap\"><table><thead><tr>")
                    .append("<th>Scenario</th><th class=\"browser\">").append(escape(browserName(Config.browser()))).append("</th></tr></thead><tbody>");
            for (Entry entry : module.getValue()) {
                String outcome = outcome(entry.status());
                html.append("<tr><td class=\"test\"><span class=\"name\">").append(escape(entry.name())).append("</span><span class=\"file\">")
                        .append(escape(entry.featureFile() + ":" + entry.line())).append("</span></td><td class=\"result\"><span class=\"pill ")
                        .append(outcome).append("\">").append(capitalize(outcome)).append("</span><span class=\"duration\">")
                        .append(escape(formatDuration(entry.duration()))).append("</span></td></tr>");
            }
            html.append("</tbody></table></div></section>\n");
        }

        html.append("<footer>Pass rate counts passed scenarios out of those that ran. Generated by ")
                .append("src/test/java/com/qualitylab/reporting/SummaryReportPlugin.java.</footer>\n</main>\n</body>\n</html>\n");
        return html.toString();
    }

    private static String tile(String kind, long value, String label) {
        return "<div class=\"tile " + kind + "\"><div class=\"value\">" + value + "</div><div class=\"label\">" + label + "</div></div>";
    }

    // ----- PDF and opening the report -----

    /** Prints the HTML to an A4 PDF with headless Chrome, using WebDriver's print command. */
    private static void printPdf(Path html, Path pdf) throws IOException {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu");
        ChromeDriver driver = new ChromeDriver(options);
        try {
            driver.get(html.toAbsolutePath().toUri().toString());
            PrintOptions print = new PrintOptions();
            print.setBackground(true);
            print.setPageSize(new PageSize(29.7, 21.0)); // A4, in centimetres: height, width
            print.setPageMargin(new PageMargin(1.4, 1.4, 1.2, 1.2));
            Pdf result = driver.print(print);
            Files.write(pdf, Base64.getDecoder().decode(result.getContent()));
        } finally {
            driver.quit();
        }
    }

    /** OPEN_REPORT: always (default), on-failure, or never. Never on CI. */
    private boolean shouldOpen() {
        String ci = System.getenv("CI");
        if (ci != null && !ci.isBlank()) {
            return false;
        }
        String mode = Config.get("OPEN_REPORT").orElse("always").toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "never", "false" -> false;
            case "on-failure" -> entries.stream().anyMatch(e -> outcome(e.status()).equals("failed"));
            default -> true;
        };
    }

    /** Opens the report in Google Chrome, or in the default browser when Chrome is not found. */
    private static void openInChrome(Path html) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String url = html.toUri().toString();
        Optional<String> chrome = Config.get("CHROME_PATH").or(() -> findChromeOnWindows(os));
        List<String> command;
        if (os.contains("win")) {
            command = chrome.map(path -> List.of(path, url)).orElse(List.of("cmd", "/c", "start", "", html.toString()));
        } else if (os.contains("mac")) {
            command = List.of("open", "-a", chrome.orElse("Google Chrome"), html.toString());
        } else {
            command = List.of(chrome.orElse("google-chrome"), url);
        }
        try {
            new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException error) {
            LOG.warn("Could not open the report in a browser: {}", error.getMessage());
        }
    }

    private static Optional<String> findChromeOnWindows(String os) {
        if (!os.contains("win")) {
            return Optional.empty();
        }
        List<String> roots = new ArrayList<>();
        for (String variable : List.of("ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA")) {
            String root = System.getenv(variable);
            if (root != null) {
                roots.add(root);
            }
        }
        return roots.stream()
                .map(root -> Paths.get(root, "Google", "Chrome", "Application", "chrome.exe"))
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .findFirst();
    }

    // ----- Small helpers -----

    private static String outcome(Status status) {
        return switch (status) {
            case PASSED -> "passed";
            case SKIPPED -> "skipped";
            default -> "failed"; // FAILED, UNDEFINED, AMBIGUOUS, PENDING, UNUSED
        };
    }

    /** "banking.feature" -> "Banking" */
    private static String module(String featureFile) {
        String name = featureFile.replace(".feature", "");
        return switch (name) {
            case "ecommerce" -> "E-commerce";
            case "banking" -> "Banking";
            case "travel" -> "Travel";
            default -> capitalize(name);
        };
    }

    private static String browserName(String browser) {
        return switch (browser) {
            case "chrome" -> "Chrome";
            case "edge" -> "Edge";
            case "firefox" -> "Firefox";
            default -> capitalize(browser);
        };
    }

    /** WebDriver errors end with long build and session details; the report keeps the useful part. */
    private static String shorten(String message) {
        String text = message;
        for (String marker : List.of("\nBuild info:", "\nSystem info:", "\nDriver info:", "\nSession ID:")) {
            int at = text.indexOf(marker);
            if (at > 0) {
                text = text.substring(0, at);
            }
        }
        text = text.replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+", "$1[hidden]");
        return text.length() > 2_000 ? text.substring(0, 2_000) + " ..." : text.trim();
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String plural(long count, String word) {
        return count + " " + word + (count == 1 ? "" : "s");
    }

    private static String formatRate(double rate) {
        return rate == Math.rint(rate) ? String.valueOf((long) rate) : String.valueOf(rate);
    }

    private static String formatDuration(Duration duration) {
        if (duration == null) {
            return "";
        }
        long millis = duration.toMillis();
        if (millis < 60_000) {
            return String.format(Locale.ROOT, "%.1fs", millis / 1000.0);
        }
        return duration.toMinutes() + "m " + duration.toSecondsPart() + "s";
    }

    private static String displayDate(Instant instant) {
        return DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH).format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private static String stamp(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private static final String CSS = """
            :root { --ink:#16202a; --muted:#5d6b78; --line:#dde3e9; --bg:#f6f8fa; --card:#fff;
              --pass:#137a4b; --pass-bg:#e3f5ec; --fail:#b42318; --fail-bg:#fdeceb; --skip:#5d6b78; --skip-bg:#eef1f4; --accent:#0f6e6e; }
            * { box-sizing:border-box; }
            body { margin:0; background:var(--bg); color:var(--ink); font:14px/1.5 "Segoe UI", system-ui, -apple-system, Arial, sans-serif; }
            main { max-width:1100px; margin:0 auto; padding:28px 16px 48px; }
            header { display:flex; flex-wrap:wrap; gap:16px; align-items:flex-start; justify-content:space-between; margin-bottom:20px; }
            h1 { margin:0 0 4px; font-size:24px; }
            h2 { font-size:17px; margin:0 0 10px; }
            h3 { font-size:14px; margin:0 0 8px; }
            .muted { color:var(--muted); font-weight:400; font-size:13px; }
            .meta { color:var(--muted); font-size:13px; }
            .meta b { color:var(--ink); font-weight:600; }
            .actions { display:flex; gap:8px; flex-wrap:wrap; }
            .button { display:inline-block; padding:9px 16px; border-radius:6px; background:var(--accent); color:#fff; text-decoration:none; font-weight:600; }
            .button.secondary { background:var(--card); color:var(--accent); border:1px solid var(--line); }
            .button.disabled { background:var(--skip-bg); color:var(--muted); cursor:not-allowed; }
            .status { display:inline-block; margin-top:6px; padding:2px 10px; border-radius:999px; font-weight:600; font-size:12px; text-transform:uppercase; letter-spacing:.04em; }
            .status.passed { background:var(--pass-bg); color:var(--pass); }
            .status.failed { background:var(--fail-bg); color:var(--fail); }
            .tiles { display:grid; grid-template-columns:repeat(auto-fit, minmax(130px, 1fr)); gap:10px; margin-bottom:24px; }
            .tile { background:var(--card); border:1px solid var(--line); border-radius:8px; padding:12px 14px; break-inside:avoid; }
            .tile .value { font-size:24px; font-weight:700; font-variant-numeric:tabular-nums; }
            .tile .label { color:var(--muted); font-size:12px; text-transform:uppercase; letter-spacing:.04em; }
            .tile.passed .value { color:var(--pass); } .tile.failed .value { color:var(--fail); }
            section { margin-bottom:26px; }
            .table-wrap { overflow-x:auto; }
            table { width:100%; border-collapse:collapse; background:var(--card); border:1px solid var(--line); border-radius:8px; overflow:hidden; }
            th, td { padding:8px 10px; border-bottom:1px solid var(--line); text-align:left; vertical-align:top; }
            th { background:#eef2f5; font-size:12px; text-transform:uppercase; letter-spacing:.04em; color:var(--muted); }
            th.browser, td.result { width:140px; text-align:center; }
            tr { break-inside:avoid; }
            td.test .name { display:block; }
            td.test .file { display:block; color:var(--muted); font-size:12px; }
            .pill { display:inline-block; min-width:64px; padding:2px 8px; border-radius:999px; font-size:12px; font-weight:600; text-align:center; }
            .pill.passed { background:var(--pass-bg); color:var(--pass); }
            .pill.failed { background:var(--fail-bg); color:var(--fail); }
            .pill.skipped { background:var(--skip-bg); color:var(--skip); }
            .duration { display:block; color:var(--muted); font-size:11px; font-variant-numeric:tabular-nums; }
            .failure { background:var(--card); border:1px solid var(--line); border-left:4px solid var(--fail); border-radius:8px; padding:14px; margin-bottom:12px; break-inside:avoid; }
            pre { white-space:pre-wrap; word-break:break-word; background:#f3f5f7; border-radius:6px; padding:10px; font:12px/1.45 Consolas, "Cascadia Mono", monospace; margin:0 0 10px; }
            .failure img { max-width:100%; border:1px solid var(--line); border-radius:6px; }
            footer { color:var(--muted); font-size:12px; border-top:1px solid var(--line); padding-top:12px; }
            @media (max-width:640px) { th.browser, td.result { width:auto; } .tile .value { font-size:20px; } }
            @media print {
              body { background:#fff; }
              main { padding:0; max-width:none; }
              .actions { display:none !important; }
              table, .tile, .failure { border-color:#c9d1d9; }
              .failure { border-left-color:var(--fail); }
            }
            """;
}
