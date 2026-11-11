package com.qualitylab.pages.travel;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import org.openqa.selenium.WebDriver;

/** One room for the chosen dates, with the booking form. */
public class RoomPage extends BasePage {

    public RoomPage(WebDriver driver) {
        super(driver);
    }

    public BigDecimal nightlyRate() {
        return Money.parse(text(testId("room-nightly-rate")));
    }

    public void book(String leadGuestName) {
        type(testId("booking-lead-guest-input"), leadGuestName);
        click(testId("booking-submit-button"));
        waitForUrlContaining("/bookings/");
    }
}
