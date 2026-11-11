package com.qualitylab.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.qualitylab.context.TestContext;
import com.qualitylab.pages.travel.BookingPage;
import com.qualitylab.pages.travel.HotelPage;
import com.qualitylab.pages.travel.HotelsPage;
import com.qualitylab.pages.travel.RoomPage;
import com.qualitylab.utils.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelSteps {

    private static final Pattern STARS = Pattern.compile("(\\d) star");
    private final TestContext context;

    public TravelSteps(TestContext context) {
        this.context = context;
    }

    // ---- Search and filters ----

    @When("I search hotels for {string}")
    public void searchHotels(String destination) {
        new HotelsPage(context.driver()).open().destination(destination).search();
    }

    @Then("the hotel results are exactly {string} and {string}")
    public void exactlyHotels(String first, String second) {
        List<String> names = new HotelsPage(context.driver()).hotelNames();
        assertEquals(names.size(), 2, "Results: " + names);
        assertTrue(names.containsAll(List.of(first, second)), "Results: " + names);
    }

    @When("I filter hotels to at least {int} stars with the {string} amenity")
    public void filterHotels(int stars, String amenity) {
        new HotelsPage(context.driver()).open().minimumStars(stars).amenity(amenity).search();
    }

    @Then("every hotel shown has at least {int} stars")
    public void everyHotelHasStars(int stars) {
        List<String> cards = new HotelsPage(context.driver()).hotelCards();
        assertFalse(cards.isEmpty(), "The filter should still find hotels");
        for (String card : cards) {
            Matcher match = STARS.matcher(card);
            assertTrue(match.find(), "No star rating in: " + card);
            assertTrue(Integer.parseInt(match.group(1)) >= stars, "Below " + stars + " stars: " + card);
        }
    }

    @When("I search hotels with check-in {string} and check-out {string}")
    public void searchWithDates(String checkIn, String checkOut) {
        new HotelsPage(context.driver()).open().dates(checkIn, checkOut).search();
    }

    @Then("I see a date error containing {string}")
    public void dateError(String message) {
        String errors = new HotelsPage(context.driver()).filterErrors();
        assertTrue(errors.contains(message), "Expected '" + message + "' in: " + errors);
    }

    // ---- Rooms and availability ----

    @When("I view the rooms of hotel {string} for {int} nights on free dates")
    public void viewRoomsOnFreeDates(String hotelId, int nights) {
        LocalDate checkIn = TestData.freeCheckInDate();
        viewRooms(hotelId, checkIn, checkIn.plusDays(nights));
    }

    @When("I view the rooms of hotel {string} from {string} to {string}")
    public void viewRoomsOn(String hotelId, String checkIn, String checkOut) {
        viewRooms(hotelId, LocalDate.parse(checkIn), LocalDate.parse(checkOut));
    }

    private void viewRooms(String hotelId, LocalDate checkIn, LocalDate checkOut) {
        context.remember("checkIn", checkIn);
        context.remember("checkOut", checkOut);
        new HotelPage(context.driver()).open(hotelId, checkIn, checkOut, 2);
    }

    @Then("rooms {string}, {string} and {string} are available with a nightly rate")
    public void roomsAvailable(String first, String second, String third) {
        HotelPage hotel = new HotelPage(context.driver());
        assertEquals(hotel.roomIds(), List.of(first, second, third));
        for (String room : List.of(first, second, third)) {
            assertTrue(hotel.isAvailable(room), room + " should be available");
            assertTrue(hotel.nightlyRate(room).signum() > 0, room + " should have a price");
        }
    }

    @Then("room {string} is sold out")
    public void roomSoldOut(String roomId) {
        assertTrue(new HotelPage(context.driver()).isSoldOut(roomId), roomId + " should be sold out");
    }

    // ---- Booking and cancelling ----

    @Given("I have booked room {string} of hotel {string} for {int} nights as {string}")
    @When("I book room {string} of hotel {string} for {int} nights as {string}")
    public void bookRoom(String roomId, String hotelId, int nights, String leadGuest) {
        viewRoomsOnFreeDates(hotelId, nights);
        HotelPage hotel = new HotelPage(context.driver());
        context.remember("nightlyRate", hotel.nightlyRate(roomId));
        hotel.chooseRoom(roomId);
        new RoomPage(context.driver()).book(leadGuest);

        String bookingId = new BookingPage(context.driver()).bookingIdFromUrl();
        context.remember("bookingId", bookingId);
        // Cancel at the end, so repeated runs never fill the room.
        context.addCleanup("cancel booking " + bookingId, () -> {
            BookingPage cleanup = new BookingPage(context.driver()).open(bookingId);
            if (cleanup.status().equals("CONFIRMED")) {
                cleanup.cancel();
            }
        });
    }

    @Then("the booking is confirmed for {int} nights for {string}")
    public void bookingConfirmed(int nights, String leadGuest) {
        BookingPage booking = new BookingPage(context.driver());
        assertTrue(booking.showsConfirmation(), "The booking confirmation should show");
        assertTrue(booking.bookingNumber().startsWith("BKG-"), "Booking numbers start with BKG-");
        assertEquals(booking.status(), "CONFIRMED");
        assertEquals(booking.nights(), nights);
        assertEquals(booking.leadGuest(), leadGuest);
    }

    @Then("the booking total is {int} times the nightly rate")
    public void bookingTotal(int nights) {
        BigDecimal rate = context.recall("nightlyRate");
        assertEquals(new BookingPage(context.driver()).total(), rate.multiply(BigDecimal.valueOf(nights)));
    }

    @When("I cancel the booking")
    public void cancelBooking() {
        String bookingId = context.recall("bookingId");
        new BookingPage(context.driver()).open(bookingId).cancel();
    }

    @Then("the booking status is {string}")
    public void bookingStatus(String status) {
        String bookingId = context.recall("bookingId");
        assertEquals(new BookingPage(context.driver()).open(bookingId).status(), status);
    }
}
