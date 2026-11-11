package com.qualitylab.pages.ecommerce;

import com.qualitylab.pages.BasePage;
import org.openqa.selenium.WebDriver;

/** One order's details, with the two-step cancel. */
public class OrderPage extends BasePage {

    public OrderPage(WebDriver driver) {
        super(driver);
    }

    public OrderPage open(String orderId) {
        openPath("/orders/" + orderId);
        visible(testId("order-detail-title"));
        return this;
    }

    /** The order id from the address, such as /orders/clx123?placed=1. */
    public String orderIdFromUrl() {
        return currentUrl().replaceAll(".*/orders/([^/?#]+).*", "$1");
    }

    public String orderNumber() {
        return text(testId("order-detail-title"));
    }

    public String status() {
        return text(testId("order-detail-status"));
    }

    public boolean showsPlacedConfirmation() {
        return isPresent(testId("order-placed-confirmation"));
    }

    public void cancel() {
        click(testId("order-cancel-button"));
        click(testId("order-cancel-confirm"));
        waitForText(testId("order-detail-status"), "CANCELLED");
    }
}
