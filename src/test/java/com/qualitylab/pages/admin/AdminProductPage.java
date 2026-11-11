package com.qualitylab.pages.admin;

import com.qualitylab.pages.BasePage;
import org.openqa.selenium.WebDriver;

/** The admin's product page, with the inventory adjustment form. */
public class AdminProductPage extends BasePage {

    public AdminProductPage(WebDriver driver) {
        super(driver);
    }

    public AdminProductPage open(String productId) {
        openPath("/admin/products/" + productId);
        visible(testId("inventory-form"));
        return this;
    }

    public int currentStock() {
        return Integer.parseInt(text(testId("inventory-current-stock")).replaceAll("\\D", ""));
    }

    /** Adds (or, with a negative number, removes) stock, then waits for the new figure to show. */
    public int adjustStock(int delta, String note) {
        int expected = currentStock() + delta;
        type(testId("inventory-delta-input"), String.valueOf(delta));
        type(testId("inventory-note-input"), note);
        click(testId("inventory-submit-button"));
        wait.until(d -> currentStock() == expected);
        return expected;
    }
}
