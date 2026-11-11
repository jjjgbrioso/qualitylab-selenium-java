@travel
Feature: Travel hotels
  A customer chooses Travel & Cruise on the home page, signs in, and finds, books, and cancels hotel stays.
  Bookings use dates far in the future and are cancelled afterwards, so repeated runs never fill a room.

  @smoke @login
  Scenario: Sign in to Travel & Cruise with valid credentials
    Given I open the home page
    When I choose the "Travel & Cruise" module
    And I sign in with the test customer's credentials
    Then I am signed in to the "Travel & Cruise" module

  @search
  Scenario: Search hotels by destination
    Given I am signed in to the "Travel & Cruise" module as the test customer
    When I search hotels for "Kyoto"
    Then the hotel results are exactly "Sakura Garden Hotel" and "Gion Townhouse Stay"

  @filter
  Scenario: Filter hotels by stars and amenity
    Given I am signed in to the "Travel & Cruise" module as the test customer
    When I filter hotels to at least 4 stars with the "Pool" amenity
    Then every hotel shown has at least 4 stars

  @rooms
  Scenario: View a hotel's rooms for chosen dates
    Given I am signed in to the "Travel & Cruise" module as the test customer
    When I view the rooms of hotel "hotel_0002" for 3 nights on free dates
    Then rooms "room_0002_std", "room_0002_dlx" and "room_0002_fam" are available with a nightly rate

  @booking @smoke
  Scenario: Book a hotel room
    Given I am signed in to the "Travel & Cruise" module as the test customer
    When I book room "room_0002_std" of hotel "hotel_0002" for 3 nights as "Selenium Tester"
    Then the booking is confirmed for 3 nights for "Selenium Tester"
    And the booking total is 3 times the nightly rate

  @booking @negative
  Scenario: Check-out before check-in is refused
    Given I am signed in to the "Travel & Cruise" module as the test customer
    When I search hotels with check-in "2031-05-10" and check-out "2031-05-08"
    Then I see a date error containing "Check-out must be after check-in"

  @booking @negative
  Scenario: A fully booked room shows as sold out
    Given I am signed in to the "Travel & Cruise" module as the test customer
    When I view the rooms of hotel "hotel_0012" from "2030-06-12" to "2030-06-14"
    Then room "room_0012_std" is sold out

  @booking @cancel
  Scenario: Cancel a hotel booking
    Given I am signed in to the "Travel & Cruise" module as the test customer
    And I have booked room "room_0002_std" of hotel "hotel_0002" for 2 nights as "Selenium Tester"
    When I cancel the booking
    Then the booking status is "CANCELLED"
