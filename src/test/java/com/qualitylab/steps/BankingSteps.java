package com.qualitylab.steps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.qualitylab.context.TestContext;
import com.qualitylab.pages.banking.AccountPage;
import com.qualitylab.pages.banking.BankTransactionPage;
import com.qualitylab.pages.banking.BankTransactionPage.Kind;
import com.qualitylab.pages.banking.BankingDashboardPage;
import com.qualitylab.utils.Money;
import com.qualitylab.utils.TestData;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.Optional;
import org.testng.SkipException;

/**
 * Banking on the live site. Balances change as people use the demo, so these steps compare
 * before and after instead of checking fixed amounts.
 */
public class BankingSteps {

    private static final BigDecimal SINGLE_TRANSACTION_LIMIT = new BigDecimal("10000.00");
    private final TestContext context;

    public BankingSteps(TestContext context) {
        this.context = context;
    }

    private String account(String alias) {
        return TestData.get("banking", "accounts." + alias);
    }

    @Then("the total balance equals the sum of my account balances")
    public void totalMatchesAccounts() {
        BankingDashboardPage dashboard = new BankingDashboardPage(context.driver()).open();
        BigDecimal sum = dashboard.accountBalances().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertFalse(dashboard.accountBalances().isEmpty(), "The customer should have at least one account");
        assertEquals(dashboard.totalBalance(), sum);
    }

    @When("I {word} a unique amount using my {word} account")
    public void moveUniqueAmount(String action, String alias) {
        Kind kind = switch (action) {
            case "deposit" -> Kind.DEPOSIT;
            case "withdraw" -> Kind.WITHDRAWAL;
            default -> throw new IllegalArgumentException("Use deposit or withdraw, not " + action);
        };
        submit(kind, alias, TestData.uniqueSmallAmount(), null);
    }

    @When("I transfer a unique amount from my {word} account to account number {string}")
    public void transferUniqueAmount(String alias, String toAccountNumber) {
        submit(Kind.TRANSFER, alias, TestData.uniqueSmallAmount(), toAccountNumber);
    }

    private void submit(Kind kind, String alias, BigDecimal amount, String toAccountNumber) {
        String accountId = account(alias);
        context.remember("accountId", accountId);
        context.remember("amount", amount);
        context.remember("balanceBefore", new AccountPage(context.driver()).open(accountId).balance());
        BankTransactionPage page = new BankTransactionPage(context.driver(), kind).open();
        page.submit(accountId, Money.format(amount), toAccountNumber);
        context.remember("transactionPage", page);
    }

    @Then("the transaction succeeds with a reference starting with {string}")
    public void transactionSucceeds(String prefix) {
        BankTransactionPage page = context.recall("transactionPage");
        assertTrue(page.succeeded(), "Expected success but saw: " + page.errors());
        String reference = page.reference();
        assertTrue(reference.startsWith(prefix), "Reference " + reference + " should start with " + prefix);
        context.remember("reference", reference);
    }

    @Then("the account balance has gone {word} by that amount")
    public void balanceChanged(String direction) {
        BigDecimal before = context.recall("balanceBefore");
        BigDecimal amount = context.recall("amount");
        BigDecimal expected = direction.equals("up") ? before.add(amount) : before.subtract(amount);
        BankTransactionPage page = context.recall("transactionPage");
        assertEquals(page.newBalance(), expected, "New balance shown on the form");
        String accountId = context.recall("accountId");
        assertEquals(new AccountPage(context.driver()).open(accountId).balance(), expected, "Balance on the account page");
    }

    @When("I try to withdraw more than the balance of my {word} account")
    public void withdrawTooMuch(String alias) {
        String accountId = account(alias);
        BigDecimal balance = new AccountPage(context.driver()).open(accountId).balance();
        BigDecimal amount = balance.add(new BigDecimal("1.00"));
        if (amount.compareTo(SINGLE_TRANSACTION_LIMIT) > 0) {
            throw new SkipException("The balance is too high to test this under the ₱10,000.00 single-transaction limit.");
        }
        context.remember("accountId", accountId);
        context.remember("balanceBefore", balance);
        BankTransactionPage page = new BankTransactionPage(context.driver(), Kind.WITHDRAWAL).open();
        page.submit(accountId, Money.format(amount), null);
        context.remember("transactionPage", page);
    }

    @When("I try to transfer {string} from my {word} account to account number {string}")
    public void tryTransfer(String amount, String alias, String toAccountNumber) {
        String accountId = account(alias);
        context.remember("accountId", accountId);
        context.remember("balanceBefore", new AccountPage(context.driver()).open(accountId).balance());
        BankTransactionPage page = new BankTransactionPage(context.driver(), Kind.TRANSFER).open();
        page.submit(accountId, amount, toAccountNumber);
        context.remember("transactionPage", page);
    }

    @Then("the transaction is refused with {string}")
    public void refused(String message) {
        BankTransactionPage page = context.recall("transactionPage");
        assertFalse(page.succeeded(), "The transaction should be refused");
        String errors = page.errors();
        assertTrue(errors.contains(message), "Expected '" + message + "' in: " + errors);
    }

    @Then("the account balance is unchanged")
    public void balanceUnchanged() {
        String accountId = context.recall("accountId");
        BigDecimal before = context.recall("balanceBefore");
        assertEquals(new AccountPage(context.driver()).open(accountId).balance(), before);
    }

    @Then("the account history lists that transaction as a {word}")
    public void historyLists(String type) {
        String accountId = context.recall("accountId");
        String reference = context.recall("reference");
        BigDecimal amount = context.recall("amount");
        AccountPage account = new AccountPage(context.driver()).open(accountId);
        Optional<String> transactionId = account.transactionIdFor(reference);
        assertTrue(transactionId.isPresent(), "History should list " + reference);
        assertEquals(account.typeOf(transactionId.get()), type);
        assertEquals(account.amountOf(transactionId.get()), amount);
    }
}
