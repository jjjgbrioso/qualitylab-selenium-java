package com.qualitylab.pages.common;

import com.qualitylab.pages.BasePage;
import java.util.Locale;
import java.util.Map;
import org.openqa.selenium.WebDriver;

/** The landing page, where a user chooses a module before signing in. */
public class HomePage extends BasePage {

    private static final Map<String, String> MODULE_IDS = Map.of(
            "e-commerce", "ecommerce",
            "banking", "banking",
            "travel & cruise", "travel",
            "admin", "admin");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        openPath("/");
        visible(testId("home-title"));
        return this;
    }

    public void chooseModule(String moduleName) {
        click(testId("home-module-" + moduleId(moduleName)));
    }

    public static String moduleId(String moduleName) {
        String id = MODULE_IDS.get(moduleName.toLowerCase(Locale.ROOT));
        if (id == null) {
            throw new IllegalArgumentException("Unknown module '" + moduleName + "'. Use E-commerce, Banking, Travel & Cruise, or Admin.");
        }
        return id;
    }
}
