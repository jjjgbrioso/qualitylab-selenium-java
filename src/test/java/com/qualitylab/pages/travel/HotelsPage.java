package com.qualitylab.pages.travel;

import com.qualitylab.pages.BasePage;
import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Hotel search with destination, dates, guests, stars, and amenity filters. */
public class HotelsPage extends BasePage {

    public HotelsPage(WebDriver driver) {
        super(driver);
    }

    public HotelsPage open() {
        openPath("/travel/hotels");
        visible(testId("hotel-filter-form"));
        return this;
    }

    public HotelsPage destination(String destination) {
        type(testId("hotel-destination-input"), destination);
        return this;
    }

    /** Dates in YYYY-MM-DD form, typed into the native date inputs. */
    public HotelsPage dates(String checkIn, String checkOut) {
        setDate("hotel-check-in-input", checkIn);
        setDate("hotel-check-out-input", checkOut);
        return this;
    }

    public HotelsPage minimumStars(int stars) {
        select(testId("hotel-stars-select"), String.valueOf(stars));
        return this;
    }

    public HotelsPage amenity(String amenity) {
        select(testId("hotel-amenity-select"), amenity);
        return this;
    }

    /**
     * The filter form is a plain GET form, so Search reloads the page. The old results are still on
     * screen until then, so wait for the old form to go away before reading the new results.
     */
    public void search() {
        WebElement oldForm = visible(testId("hotel-filter-form"));
        click(testId("hotel-filter-submit"));
        wait.until(ExpectedConditions.stalenessOf(oldForm));
        visible(testId("hotel-filter-form"));
        wait.until(d -> isPresent(testId("hotel-grid")) || isPresent(testId("hotel-empty-state"))
                || isPresent(testIdStartingWith("hotel-filter-error-")));
    }

    public List<String> hotelNames() {
        return all(testIdStartingWith("hotel-name-")).stream().map(WebElement::getText).map(String::trim).toList();
    }

    /** The text of every result card, such as "Harborlight Hotel Lisbon, Portugal · 4 star ...". */
    public List<String> hotelCards() {
        return all(testIdStartingWith("hotel-card-")).stream().map(WebElement::getText).toList();
    }

    public String filterErrors() {
        return all(testIdStartingWith("hotel-filter-error-")).stream()
                .map(WebElement::getText).map(String::trim).collect(Collectors.joining(" | "));
    }

    /** Date inputs need the value set in the browser's own format, so this sets it through script and fires change. */
    private void setDate(String id, String isoDate) {
        WebElement input = visible(testId(id));
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const input = arguments[0];"
                        + "const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;"
                        + "setter.call(input, arguments[1]);"
                        + "input.dispatchEvent(new Event('input', { bubbles: true }));"
                        + "input.dispatchEvent(new Event('change', { bubbles: true }));",
                input, isoDate);
    }
}
