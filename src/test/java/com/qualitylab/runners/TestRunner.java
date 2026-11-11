package com.qualitylab.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Runs every feature through TestNG. Choose scenarios with tags, for example
 * {@code -Dcucumber.filter.tags="@banking and not @negative"}.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.qualitylab.steps", "com.qualitylab.hooks"},
        plugin = {
            "pretty",
            "summary",
            "html:target/cucumber-reports/cucumber.html",
            "json:target/cucumber-reports/cucumber.json",
            "com.qualitylab.reporting.ExecutionSummaryPlugin"
        },
        monochrome = true)
public class TestRunner extends AbstractTestNGCucumberTests {

    /** One scenario at a time: they share the live site's data for the test customer. */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
