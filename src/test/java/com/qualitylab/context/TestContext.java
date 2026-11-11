package com.qualitylab.context;

import com.qualitylab.driver.DriverFactory;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * State shared by the step classes of one scenario: the browser, values remembered between
 * steps, and cleanup actions. PicoContainer creates a new one for every scenario.
 */
public class TestContext {

    private static final Logger LOG = LoggerFactory.getLogger(TestContext.class);

    private WebDriver driver;
    private final Map<String, Object> values = new HashMap<>();
    private final Deque<Cleanup> cleanups = new ArrayDeque<>();

    public void startDriver() {
        driver = DriverFactory.create();
    }

    public WebDriver driver() {
        if (driver == null) {
            throw new IllegalStateException("The browser has not been started.");
        }
        return driver;
    }

    public boolean hasDriver() {
        return driver != null;
    }

    public void remember(String key, Object value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T recall(String key) {
        if (!values.containsKey(key)) {
            throw new IllegalStateException("No value was remembered under '" + key + "'.");
        }
        return (T) values.get(key);
    }

    /** Undo work a scenario did on shared data, such as cancelling a booking. Runs even when the scenario fails. */
    public void addCleanup(String description, Runnable action) {
        cleanups.push(new Cleanup(description, action));
    }

    public void runCleanups() {
        while (!cleanups.isEmpty()) {
            Cleanup cleanup = cleanups.pop();
            try {
                LOG.info("Cleanup: {}", cleanup.description());
                cleanup.action().run();
            } catch (RuntimeException error) {
                LOG.warn("Cleanup failed: {} ({})", cleanup.description(), error.getMessage());
            }
        }
    }

    public void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                driver = null;
            }
        }
    }

    private record Cleanup(String description, Runnable action) {
    }
}
