package com.qualitylab.driver;

import com.qualitylab.config.Config;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a browser for one scenario. WebDriverManager downloads the matching driver on first use. */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);
    private static final String WINDOW_SIZE = "1366,900";

    private DriverFactory() {
    }

    public static WebDriver create() {
        String browser = Config.browser();
        boolean headless = Config.headless();
        LOG.info("Starting {} ({})", browser, headless ? "headless" : "visible");

        WebDriver driver = switch (browser) {
            case "chrome" -> chrome(headless);
            case "edge" -> edge(headless);
            case "firefox" -> firefox(headless);
            default -> throw new IllegalArgumentException("Unsupported browser '" + browser + "'. Use chrome, edge, or firefox.");
        };
        // Explicit waits only: an implicit wait would slow down every "is this absent?" check.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        if (!headless) {
            driver.manage().window().maximize();
        }
        return driver;
    }

    private static WebDriver chrome(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=" + WINDOW_SIZE, "--disable-search-engine-choice-screen", "--no-first-run");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return new ChromeDriver(options);
    }

    private static WebDriver edge(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--window-size=" + WINDOW_SIZE, "--no-first-run");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return new EdgeDriver(options);
    }

    private static WebDriver firefox(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addArguments("--width=1366", "--height=900");
        return new FirefoxDriver(options);
    }
}
