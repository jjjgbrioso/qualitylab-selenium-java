package com.qualitylab.pages.ecommerce;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** The cart. Quantities save when the field loses focus. */
public class CartPage extends BasePage {

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage open() {
        openPath("/cart");
        visible(testId("cart-title"));
        // The page shows either cart lines or the empty-cart message once it has loaded.
        wait.until(d -> isPresent(testId("cart-summary")) || isPresent(testId("cart-empty-state")));
        return this;
    }

    /** Removes every line, so a scenario starts from a known cart. */
    public void empty() {
        open();
        By remove = testIdStartingWith("cart-item-remove-");
        while (isPresent(remove)) {
            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(remove));
            button.click();
            wait.until(ExpectedConditions.stalenessOf(button));
        }
        visible(testId("cart-empty-state"));
    }

    public boolean isEmpty() {
        return isPresent(testId("cart-empty-state"));
    }

    public boolean contains(String productId) {
        return isPresent(testId("cart-item-" + productId));
    }

    public int quantity(String productId) {
        return Integer.parseInt(visible(testId("cart-item-quantity-" + productId)).getDomProperty("value"));
    }

    public BigDecimal lineTotal(String productId) {
        return Money.parse(text(testId("cart-item-total-" + productId)));
    }

    public void changeQuantity(String productId, int quantity) {
        BigDecimal before = lineTotal(productId);
        WebElement field = visible(testId("cart-item-quantity-" + productId));
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), String.valueOf(quantity), Keys.TAB);
        wait.until(d -> !lineTotal(productId).equals(before));
    }

    public void remove(String productId) {
        click(testId("cart-item-remove-" + productId));
        waitUntilGone(testId("cart-item-" + productId));
    }

    public void proceedToCheckout() {
        click(testId("cart-checkout-link"));
        waitForUrlContaining("/checkout");
    }
}
