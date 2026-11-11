package com.qualitylab.pages.ecommerce;

import com.qualitylab.pages.BasePage;
import org.openqa.selenium.WebDriver;

/** The customer's order history. */
public class OrdersPage extends BasePage {

    public OrdersPage(WebDriver driver) {
        super(driver);
    }

    public OrdersPage open() {
        openPath("/orders");
        visible(testId("orders-title"));
        return this;
    }

    public boolean lists(String orderId) {
        return isPresent(testId("order-row-" + orderId));
    }

    public String rowText(String orderId) {
        return text(testId("order-row-" + orderId));
    }

    public String status(String orderId) {
        return text(testId("order-status-" + orderId));
    }
}
