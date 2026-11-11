package com.qualitylab.pages.banking;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import java.util.Optional;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** One bank account: balance and transaction history, newest first. */
public class AccountPage extends BasePage {

    public AccountPage(WebDriver driver) {
        super(driver);
    }

    public AccountPage open(String accountId) {
        openPath("/banking/accounts/" + accountId);
        visible(testId("account-balance"));
        return this;
    }

    public BigDecimal balance() {
        return Money.parse(text(testId("account-balance")));
    }

    /** The row id of the first transaction with this reference, if listed on the first page. */
    public Optional<String> transactionIdFor(String reference) {
        visible(testId("transactions-table"));
        return all(testIdStartingWith("transaction-reference-")).stream()
                .filter(cell -> cell.getText().trim().equals(reference))
                .map(cell -> cell.getDomAttribute("data-testid").replace("transaction-reference-", ""))
                .findFirst();
    }

    public String typeOf(String transactionId) {
        return text(testId("transaction-type-" + transactionId));
    }

    public BigDecimal amountOf(String transactionId) {
        WebElement amount = visible(testId("transaction-amount-" + transactionId));
        return Money.parse(amount.getText()).abs();
    }
}
