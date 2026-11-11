package com.qualitylab.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.qualitylab.config.Config;
import com.qualitylab.context.TestContext;
import com.qualitylab.pages.common.HomePage;
import com.qualitylab.pages.common.LoginPage;
import com.qualitylab.pages.common.NavBar;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

/** Signing in. Every module starts the same way: home page, choose the module, sign in. */
public class CommonSteps {

    private final TestContext context;

    public CommonSteps(TestContext context) {
        this.context = context;
    }

    @Given("I open the home page")
    public void openHomePage() {
        new HomePage(context.driver()).open();
    }

    @When("I choose the {string} module")
    public void chooseModule(String module) {
        new HomePage(context.driver()).chooseModule(module);
        String title = new LoginPage(context.driver()).title();
        assertEquals(title, "Sign in to " + module, "The module's login page should open");
    }

    @When("I sign in with the test customer's credentials")
    public void signInAsCustomer() {
        new LoginPage(context.driver()).signIn(Config.required(Config.TEST_USERNAME), Config.required(Config.TEST_PASSWORD));
    }

    @When("I sign in with the test customer's email and the password {string}")
    public void signInWithPassword(String password) {
        new LoginPage(context.driver()).signIn(Config.required(Config.TEST_USERNAME), password);
    }

    @Then("I am signed in to the {string} module")
    public void signedInTo(String module) {
        NavBar nav = new NavBar(context.driver());
        nav.waitForModule(module);
        assertEquals(nav.currentModule(), module);
    }

    @Then("I see the sign-in error {string}")
    public void signInError(String message) {
        assertEquals(new LoginPage(context.driver()).error(), message);
    }

    @Given("I am signed in to the {string} module as the test customer")
    public void signedInAsCustomer(String module) {
        signInTo(module, Config.required(Config.TEST_USERNAME), Config.required(Config.TEST_PASSWORD));
    }

    @Given("I am signed in to the {string} module as the admin")
    public void signedInAsAdmin(String module) {
        // Admin scenarios need their own account. Without one, they are skipped rather than failed.
        if (Config.get(Config.ADMIN_USERNAME).isEmpty() || Config.get(Config.ADMIN_PASSWORD).isEmpty()) {
            throw new SkipException("ADMIN_USERNAME and ADMIN_PASSWORD are not set, so admin scenarios are skipped.");
        }
        signInTo(module, Config.required(Config.ADMIN_USERNAME), Config.required(Config.ADMIN_PASSWORD));
    }

    private void signInTo(String module, String email, String password) {
        openHomePage();
        chooseModule(module);
        new LoginPage(context.driver()).signIn(email, password);
        signedInTo(module);
        assertTrue(context.driver().getCurrentUrl().startsWith(Config.baseUrl()), "Signing in should stay on the site");
    }
}
