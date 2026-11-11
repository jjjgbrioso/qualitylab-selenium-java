package com.qualitylab.hooks;

import com.qualitylab.config.Config;
import com.qualitylab.context.TestContext;
import com.qualitylab.utils.ScreenshotUtil;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Starts a fresh browser for each scenario, and on the way out captures failures and cleans up. */
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);
    private final TestContext context;

    public Hooks(TestContext context) {
        this.context = context;
    }

    @Before(order = 0)
    public void startBrowser(Scenario scenario) {
        LOG.info("=== Scenario: {} {} [{} at {}]", scenario.getName(), scenario.getSourceTagNames(), Config.environment(), Config.baseUrl());
        context.startDriver();
    }

    @After(order = 0)
    public void finish(Scenario scenario) {
        try {
            if (scenario.isFailed() && context.hasDriver()) {
                captureFailure(scenario);
            }
            context.runCleanups();
        } finally {
            context.quitDriver();
            LOG.info("=== {}: {}", scenario.getStatus(), scenario.getName());
        }
    }

    private void captureFailure(Scenario scenario) {
        try {
            byte[] png = ((TakesScreenshot) context.driver()).getScreenshotAs(OutputType.BYTES);
            scenario.attach(png, "image/png", "Failure screenshot");
            String path = ScreenshotUtil.save(png, scenario.getName());
            LOG.error("Failed at {}. Screenshot: {}", context.driver().getCurrentUrl(), path);
        } catch (RuntimeException error) {
            LOG.warn("Could not take a failure screenshot: {}", error.getMessage());
        }
    }
}
