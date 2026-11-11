package com.qualitylab.pages.travel;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.openqa.selenium.WebDriver;

/** One hotel, with room availability for the chosen stay. */
public class HotelPage extends BasePage {

    public HotelPage(WebDriver driver) {
        super(driver);
    }

    public HotelPage open(String hotelId, LocalDate checkIn, LocalDate checkOut, int guests) {
        openPath("/travel/hotels/" + hotelId + "?checkIn=" + checkIn + "&checkOut=" + checkOut + "&guests=" + guests);
        visible(testId("room-list"));
        return this;
    }

    public List<String> roomIds() {
        return all(testIdStartingWith("room-card-")).stream()
                .map(card -> card.getDomAttribute("data-testid").replace("room-card-", ""))
                .toList();
    }

    public BigDecimal nightlyRate(String roomId) {
        return Money.parse(text(testId("room-price-" + roomId)));
    }

    public boolean isSoldOut(String roomId) {
        return isPresent(testId("room-sold-out-" + roomId));
    }

    public boolean isAvailable(String roomId) {
        return isPresent(testId("room-available-" + roomId));
    }

    public void chooseRoom(String roomId) {
        click(testId("room-view-" + roomId));
        waitForUrlContaining("/travel/rooms/" + roomId);
    }
}
