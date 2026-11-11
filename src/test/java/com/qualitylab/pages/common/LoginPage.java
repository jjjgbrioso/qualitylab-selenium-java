package com.qualitylab.pages.common;

import com.qualitylab.pages.BasePage;
import org.openqa.selenium.WebDriver;

/** The sign-in page. It signs in to the module of the page the user was opening. */
public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public String title() {
        return text(testId("login-title"));
    }

    public void signIn(String email, String password) {
        type(testId("login-email-input"), email);
        type(testId("login-password-input"), password);
        click(testId("login-submit-button"));
    }

    public String error() {
        return text(testId("login-error"));
    }
}
