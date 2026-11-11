package com.qualitylab.pages.travel;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import org.openqa.selenium.WebDriver;

/** One hotel booking's details, with the two-step cancel. */
public class BookingPage extends BasePage {

    public BookingPage(WebDriver driver) {
        super(driver);
    }

    public BookingPage open(String bookingId) {
        openPath("/bookings/" + bookingId);
        visible(testId("booking-detail-title"));
        return this;
    }

    public String bookingIdFromUrl() {
        return currentUrl().replaceAll(".*/bookings/([^/?#]+).*", "$1");
    }

    public String bookingNumber() {
        return text(testId("booking-detail-title"));
    }

    public boolean showsConfirmation() {
        return isPresent(testId("booking-confirmation"));
    }

    public String status() {
        return text(testId("booking-detail-status"));
    }

    public int nights() {
        return Integer.parseInt(text(testId("booking-detail-nights")).replaceAll("\\D", ""));
    }

    public BigDecimal total() {
        return Money.parse(text(testId("booking-detail-total")));
    }

    public String leadGuest() {
        return text(testId("booking-detail-lead-guest"));
    }

    public void cancel() {
        click(testId("booking-cancel-button"));
        click(testId("booking-cancel-confirm"));
        waitForText(testId("booking-detail-status"), "CANCELLED");
    }
}
