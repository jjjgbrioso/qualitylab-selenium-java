package com.qualitylab.pages.common;

import com.qualitylab.pages.BasePage;
import org.openqa.selenium.WebDriver;

/** The top navigation. Once signed in, it names the one module the session can use. */
public class NavBar extends BasePage {

    public NavBar(WebDriver driver) {
        super(driver);
    }

    public String currentModule() {
        return text(testId("nav-current-module"));
    }

    public void waitForModule(String moduleName) {
        waitForText(testId("nav-current-module"), moduleName);
    }
}
