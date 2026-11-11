package com.qualitylab.pages;

import com.qualitylab.config.Config;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared page behaviour. Elements are found by their {@code data-testid}, which the app keeps stable
 * for automation. Every lookup waits explicitly; there are no sleeps.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(Config.timeoutSeconds()));
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    protected static By testIdStartingWith(String prefix) {
        return By.cssSelector("[data-testid^='" + prefix + "']");
    }

    protected void openPath(String path) {
        driver.get(Config.baseUrl() + path);
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Buttons stay disabled until the page is interactive, so waiting for "clickable" also waits for
     * the page to finish loading its scripts.
     */
    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement field = visible(locator);
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        field.sendKeys(text);
    }

    protected void select(By locator, String value) {
        new Select(visible(locator)).selectByValue(value);
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected List<WebElement> all(By locator) {
        return driver.findElements(locator);
    }

    protected void waitForUrlContaining(String fragment) {
        wait.until(ExpectedConditions.urlContains(fragment));
    }

    protected void waitForText(By locator, String expected) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, expected));
    }

    protected void waitUntilGone(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }
}
