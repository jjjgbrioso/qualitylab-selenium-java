@ecommerce
Feature: E-commerce
  A customer chooses E-commerce on the home page, signs in, and shops: search, product details,
  cart, checkout, and order history. An admin keeps the inventory up to date.

  @smoke @login
  Scenario: Sign in to E-commerce with valid credentials
    Given I open the home page
    When I choose the "E-commerce" module
    And I sign in with the test customer's credentials
    Then I am signed in to the "E-commerce" module

  @login @negative
  Scenario: Sign-in is refused with a wrong password
    Given I open the home page
    When I choose the "E-commerce" module
    And I sign in with the test customer's email and the password "Wrong@12345"
    Then I see the sign-in error "The email or password is incorrect."

  @search
  Scenario: Search for a product
    Given I am signed in to the "E-commerce" module as the test customer
    When I search the shop for "keyboard"
    Then the search results include "Vector Mechanical Keyboard"

  @product
  Scenario: View a product's details
    Given I am signed in to the "E-commerce" module as the test customer
    When I open the product "prod_0009"
    Then the product name is "Willow Knit Throw"
    And the product price is "$69.99" and the original price is "$99.99"

  @cart
  Scenario: Add a product to the cart
    Given I am signed in to the "E-commerce" module as the test customer
    And my cart is empty
    When I add 1 of product "prod_0027" to the cart
    Then my cart contains product "prod_0027" with quantity 1

  @cart
  Scenario: Update the quantity in the cart
    Given I am signed in to the "E-commerce" module as the test customer
    And my cart is empty
    And I add 1 of product "prod_0027" to the cart
    When I change the quantity of product "prod_0027" to 3
    Then the line total for product "prod_0027" is 3 times its unit price

  @cart
  Scenario: Remove an item from the cart
    Given I am signed in to the "E-commerce" module as the test customer
    And my cart is empty
    And I add 1 of product "prod_0027" to the cart
    When I remove product "prod_0027" from the cart
    Then my cart has no items

  @checkout @smoke
  Scenario: Check out and find the order in the order history
    Given I am signed in to the "E-commerce" module as the test customer
    And my cart is empty
    And I add 1 of product "prod_0027" to the cart
    When I check out with the approved test card
    Then the order is placed with status "PAID"
    And the order appears in my order history with status "PAID"
    And my cart is empty

  @admin
  Scenario: Admin updates a product's inventory
    Given I am signed in to the "Admin" module as the admin
    When I add 5 units to the stock of product "prod_0028"
    Then the stock of product "prod_0028" has gone up by 5
