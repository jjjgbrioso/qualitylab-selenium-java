package com.qualitylab.pages.ecommerce;

import com.qualitylab.pages.BasePage;
import java.util.Map;
import org.openqa.selenium.WebDriver;

/** Shipping and simulated card payment. Only the documented test cards are accepted. */
public class CheckoutPage extends BasePage {

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    /** Fills each field from a map keyed by the field's test id, such as "checkout-full-name". */
    public void fill(Map<String, String> fields) {
        visible(testId("checkout-form"));
        fields.forEach((field, value) -> type(testId(field + "-input"), value));
    }

    public void placeOrder() {
        click(testId("checkout-submit-button"));
        waitForUrlContaining("/orders/");
    }
}
