package com.qualitylab.pages.banking;

import com.qualitylab.pages.BasePage;
import com.qualitylab.utils.Money;
import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.WebDriver;

/** The banking dashboard: total balance and one card per account. */
public class BankingDashboardPage extends BasePage {

    public BankingDashboardPage(WebDriver driver) {
        super(driver);
    }

    public BankingDashboardPage open() {
        openPath("/banking");
        visible(testId("banking-total-balance"));
        return this;
    }

    public BigDecimal totalBalance() {
        return Money.parse(text(testId("banking-total-balance")));
    }

    public List<BigDecimal> accountBalances() {
        return all(testIdStartingWith("account-balance-")).stream().map(element -> Money.parse(element.getText())).toList();
    }

    public BigDecimal balanceOf(String accountId) {
        return Money.parse(text(testId("account-balance-" + accountId)));
    }
}
