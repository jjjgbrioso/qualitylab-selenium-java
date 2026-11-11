package com.qualitylab.pages.ecommerce;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import org.openqa.selenium.WebDriver;

/** One product: details, price, and the add-to-cart form. */
public class ProductPage extends BasePage {

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    public ProductPage open(String productId) {
        openPath("/shop/" + productId);
        visible(testId("product-detail"));
        return this;
    }

    public String name() {
        return text(testId("product-detail-name"));
    }

    public String price() {
        return text(testId("product-detail-price"));
    }

    public BigDecimal priceAmount() {
        return Money.parse(price());
    }

    public String listPrice() {
        return text(testId("product-detail-list-price")).replace("Was", "").trim();
    }

    public void addToCart(int quantity) {
        type(testId("product-quantity-input"), String.valueOf(quantity));
        click(testId("add-to-cart-button"));
        visible(testId("add-to-cart-success"));
    }
}
