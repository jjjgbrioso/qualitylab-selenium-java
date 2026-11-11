package com.qualitylab.pages.ecommerce;

import com.qualitylab.pages.BasePage;
import java.util.List;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** The marketplace: search, filters, and the product grid. */
public class ShopPage extends BasePage {

    public ShopPage(WebDriver driver) {
        super(driver);
    }

    public ShopPage open() {
        openPath("/shop");
        visible(testId("shop-header"));
        return this;
    }

    public void search(String term) {
        type(testId("product-search-input"), term);
        click(testId("product-search-button"));
        waitForUrlContaining("q=");
        visible(testId("product-result-count"));
    }

    public List<String> productNames() {
        visible(testId("product-grid"));
        return all(testIdStartingWith("product-name-")).stream().map(WebElement::getText).map(String::trim).toList();
    }
}
