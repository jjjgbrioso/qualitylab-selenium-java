package com.qualitylab.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.qualitylab.context.TestContext;
import com.qualitylab.pages.admin.AdminProductPage;
import com.qualitylab.pages.ecommerce.CartPage;
import com.qualitylab.pages.ecommerce.CheckoutPage;
import com.qualitylab.pages.ecommerce.OrderPage;
import com.qualitylab.pages.ecommerce.OrdersPage;
import com.qualitylab.pages.ecommerce.ProductPage;
import com.qualitylab.pages.ecommerce.ShopPage;
import com.qualitylab.utils.TestData;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EcommerceSteps {

    private final TestContext context;

    public EcommerceSteps(TestContext context) {
        this.context = context;
    }

    // ---- Search and product details ----

    @When("I search the shop for {string}")
    public void searchShop(String term) {
        ShopPage shop = new ShopPage(context.driver()).open();
        shop.search(term);
    }

    @Then("the search results include {string}")
    public void resultsInclude(String productName) {
        List<String> names = new ShopPage(context.driver()).productNames();
        assertTrue(names.contains(productName), "Expected " + productName + " in " + names);
    }

    @When("I open the product {string}")
    public void openProduct(String productId) {
        new ProductPage(context.driver()).open(productId);
    }

    @Then("the product name is {string}")
    public void productName(String name) {
        assertEquals(new ProductPage(context.driver()).name(), name);
    }

    @Then("the product price is {string} and the original price is {string}")
    public void productPrices(String price, String listPrice) {
        ProductPage product = new ProductPage(context.driver());
        assertEquals(product.price(), price);
        assertEquals(product.listPrice(), listPrice);
    }

    // ---- Cart ----

    @Given("my cart is empty")
    public void emptyCart() {
        new CartPage(context.driver()).empty();
    }

    @When("I add {int} of product {string} to the cart")
    public void addToCart(int quantity, String productId) {
        ProductPage product = new ProductPage(context.driver()).open(productId);
        context.remember("unitPrice", product.priceAmount());
        product.addToCart(quantity);
    }

    @Then("my cart contains product {string} with quantity {int}")
    public void cartContains(String productId, int quantity) {
        CartPage cart = new CartPage(context.driver()).open();
        assertTrue(cart.contains(productId), "The cart should list " + productId);
        assertEquals(cart.quantity(productId), quantity);
    }

    @When("I change the quantity of product {string} to {int}")
    public void changeQuantity(String productId, int quantity) {
        new CartPage(context.driver()).open().changeQuantity(productId, quantity);
    }

    @Then("the line total for product {string} is {int} times its unit price")
    public void lineTotal(String productId, int quantity) {
        BigDecimal unitPrice = context.recall("unitPrice");
        CartPage cart = new CartPage(context.driver());
        assertEquals(cart.quantity(productId), quantity);
        assertEquals(cart.lineTotal(productId), unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    @When("I remove product {string} from the cart")
    public void removeFromCart(String productId) {
        new CartPage(context.driver()).open().remove(productId);
    }

    @Then("my cart has no items")
    public void cartIsEmpty() {
        assertTrue(new CartPage(context.driver()).open().isEmpty(), "The cart should be empty");
    }

    // ---- Checkout and orders ----

    @When("I check out with the approved test card")
    public void checkOut() {
        CartPage cart = new CartPage(context.driver()).open();
        cart.proceedToCheckout();

        Map<String, String> fields = new LinkedHashMap<>();
        for (String field : List.of("full-name", "address", "city", "postcode", "country", "card-name", "card-number", "card-expiry", "card-cvv")) {
            fields.put("checkout-" + field, TestData.get("ecommerce", "checkout." + field));
        }
        CheckoutPage checkout = new CheckoutPage(context.driver());
        checkout.fill(fields);
        checkout.placeOrder();

        OrderPage order = new OrderPage(context.driver());
        String orderId = order.orderIdFromUrl();
        context.remember("orderId", orderId);
        // Cancelling afterwards returns the stock, so repeated runs do not drain it.
        context.addCleanup("cancel order " + orderId, () -> {
            OrderPage cleanup = new OrderPage(context.driver()).open(orderId);
            if (cleanup.status().equals("PAID")) {
                cleanup.cancel();
            }
        });
    }

    @Then("the order is placed with status {string}")
    public void orderPlaced(String status) {
        OrderPage order = new OrderPage(context.driver());
        assertTrue(order.showsPlacedConfirmation(), "The order confirmation should show");
        assertTrue(order.orderNumber().startsWith("ORD-"), "Order numbers start with ORD-");
        assertEquals(order.status(), status);
    }

    @Then("the order appears in my order history with status {string}")
    public void orderInHistory(String status) {
        String orderId = context.recall("orderId");
        OrdersPage orders = new OrdersPage(context.driver()).open();
        assertTrue(orders.lists(orderId), "Order history should list the new order");
        assertEquals(orders.status(orderId), status);
    }

    // ---- Admin inventory ----

    @When("I add {int} units to the stock of product {string}")
    public void addStock(int units, String productId) {
        AdminProductPage page = new AdminProductPage(context.driver()).open(productId);
        int before = page.currentStock();
        context.remember("stockBefore", before);
        page.adjustStock(units, "Selenium inventory check");
        context.addCleanup("put the stock of " + productId + " back", () ->
                new AdminProductPage(context.driver()).open(productId).adjustStock(-units, "Selenium inventory check: undo"));
    }

    @Then("the stock of product {string} has gone up by {int}")
    public void stockWentUp(String productId, int units) {
        int before = context.recall("stockBefore");
        int after = new AdminProductPage(context.driver()).open(productId).currentStock();
        assertEquals(after, before + units);
        assertFalse(after < 0, "Stock is never negative");
    }
}
