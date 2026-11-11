package com.qualitylab.reporting;

import com.qualitylab.config.Config;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes target/execution-summary.txt: counts by result, failures with their reason, and run settings. */
public class ExecutionSummaryPlugin implements ConcurrentEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionSummaryPlugin.class);
    private static final Path OUTPUT = Paths.get("target", "execution-summary.txt");

    private final Map<Status, Integer> counts = new EnumMap<>(Status.class);
    private final List<String> failures = new ArrayList<>();
    private Instant started = Instant.now();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, event -> started = event.getInstant());
        publisher.registerHandlerFor(TestCaseFinished.class, this::onScenarioFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    private synchronized void onScenarioFinished(TestCaseFinished event) {
        Status status = event.getResult().getStatus();
        counts.merge(status, 1, Integer::sum);
        if (status == Status.FAILED) {
            String reason = event.getResult().getError() == null ? "" : event.getResult().getError().getMessage();
            failures.add(event.getTestCase().getName() + " (" + event.getTestCase().getUri().getPath().replaceAll(".*/", "")
                    + ":" + event.getTestCase().getLocation().getLine() + ")\n      " + firstLine(reason));
        }
    }

    private synchronized void onRunFinished(TestRunFinished event) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        Duration duration = Duration.between(started, event.getInstant());
        StringBuilder summary = new StringBuilder()
                .append("QualityLab UI test run\n")
                .append("Environment: ").append(Config.environment()).append(" (").append(Config.baseUrl()).append(")\n")
                .append("Browser:     ").append(Config.browser()).append(Config.headless() ? ", headless" : "").append('\n')
                .append("Duration:    ").append(duration.toMinutesPart()).append("m ").append(duration.toSecondsPart()).append("s\n")
                .append("Scenarios:   ").append(total)
                .append(" | passed ").append(counts.getOrDefault(Status.PASSED, 0))
                .append(" | failed ").append(counts.getOrDefault(Status.FAILED, 0))
                .append(" | skipped ").append(counts.getOrDefault(Status.SKIPPED, 0))
                .append(" | other ").append(total - counts.getOrDefault(Status.PASSED, 0)
                        - counts.getOrDefault(Status.FAILED, 0) - counts.getOrDefault(Status.SKIPPED, 0))
                .append('\n');
        if (!failures.isEmpty()) {
            summary.append("\nFailed scenarios:\n");
            failures.forEach(failure -> summary.append("  - ").append(failure).append('\n'));
            summary.append("\nScreenshots: target/screenshots\n");
        }
        summary.append("Reports:     target/cucumber-reports/cucumber.html, target/surefire-reports/index.html\n");

        try {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, summary.toString(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        LOG.info("\n{}", summary);
    }

    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int newline = text.indexOf('\n');
        return newline < 0 ? text : text.substring(0, newline);
    }
}
