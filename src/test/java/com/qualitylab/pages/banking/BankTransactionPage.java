package com.qualitylab.pages.banking;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** The deposit, withdrawal, and transfer forms, which share one layout. */
public class BankTransactionPage extends BasePage {

    public enum Kind {
        DEPOSIT("/banking/deposit", "deposit"),
        WITHDRAWAL("/banking/withdraw", "withdrawal"),
        TRANSFER("/banking/transfer", "transfer");

        private final String path;
        private final String prefix;

        Kind(String path, String prefix) {
            this.path = path;
            this.prefix = prefix;
        }
    }

    private final Kind kind;

    public BankTransactionPage(WebDriver driver, Kind kind) {
        super(driver);
        this.kind = kind;
    }

    public BankTransactionPage open() {
        openPath(kind.path);
        visible(testId(kind.prefix + "-form"));
        return this;
    }

    public void submit(String accountId, String amount, String toAccountNumber) {
        select(testId(kind.prefix + "-account-select"), accountId);
        if (kind == Kind.TRANSFER) {
            type(testId("transfer-to-account-input"), toAccountNumber);
        }
        type(testId(kind.prefix + "-amount-input"), amount);
        click(testId(kind.prefix + "-submit-button"));
        wait.until(d -> isPresent(testId(kind.prefix + "-success")) || !errors().isEmpty());
    }

    public boolean succeeded() {
        return isPresent(testId(kind.prefix + "-success"));
    }

    public String reference() {
        return text(testId(kind.prefix + "-reference"));
    }

    public BigDecimal newBalance() {
        return Money.parse(text(testId(kind.prefix + "-new-balance")));
    }

    /** The form's error message and any field errors, as one string. */
    public String errors() {
        return all(testIdStartingWith(kind.prefix + "-")).stream()
                .filter(element -> element.getDomAttribute("data-testid").endsWith("-error"))
                .map(WebElement::getText)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining(" | "));
    }
}
