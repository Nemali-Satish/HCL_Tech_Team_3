package pageObjects;

import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * BasePage - shared wait/action helpers for ALL page objects.
 * Branch: feature/member1-infra
 *
 * Design decisions (from design doc §5 & §7):
 *  - All wait timeouts configurable; never hardcoded
 *  - JS click fallback for ElementClickInterceptedException (Edge Case #8)
 *  - scrollIntoView before click-sensitive elements (Edge Cases #12, #18)
 *  - StaleElement retry logic (Edge Case #9)
 */
public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected WebDriverWait shortWait;    // 3-5s for optional popups
    protected JavascriptExecutor js;
    protected Logger logger;

    /**
     * @param driver          live WebDriver instance
     * @param explicitTimeout seconds for standard explicit wait  (from config: explicitWait)
     * @param shortTimeout    seconds for optional-element waits  (from config: shortWait)
     */
    public BasePage(WebDriver driver, int explicitTimeout, int shortTimeout) {
        this.driver = driver;
        this.wait      = new WebDriverWait(driver, Duration.ofSeconds(explicitTimeout));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(shortTimeout));
        this.js        = (JavascriptExecutor) driver;
        this.logger    = LogManager.getLogger(this.getClass());
        PageFactory.initElements(driver, this);
    }

    // ── Wait helpers ───────────────────────────────────────────────────────

    /** Wait until element is visible in DOM */
    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Wait until element is clickable (visible + enabled) */
    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Short wait for optional elements (popups etc.) - does NOT throw if absent */
    protected WebElement shortWaitForVisibility(By locator) {
        return shortWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Wait until element disappears (e.g. loading spinner) */
    protected boolean waitForInvisibility(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ── Click helpers ──────────────────────────────────────────────────────

    /**
     * Safe click: tries normal click first, falls back to JS click.
     * Handles ElementClickInterceptedException (Edge Case #8 - overlay blocking click).
     */
    protected void safeClick(WebElement element) {
        try {
            scrollIntoView(element);
            waitForClickable(element);
            element.click();
            logger.debug("Clicked element: {}", element);
        } catch (ElementClickInterceptedException e) {
            logger.warn("Normal click intercepted; falling back to JS click. Element: {}", element);
            jsClick(element);
        }
    }

    /**
     * Waits for clickable state on a By locator, then safe-clicks.
     */
    protected void safeClick(By locator) {
        WebElement element = waitForClickable(locator);
        safeClick(element);
    }

    /** Direct JavaScript click bypass - use when overlays block normal click */
    protected void jsClick(WebElement element) {
        js.executeScript("arguments[0].click();", element);
        logger.debug("JS click executed on: {}", element);
    }

    // ── Scroll helpers ─────────────────────────────────────────────────────

    /** Scroll element into viewport (Edge Cases #12, #18 - filters and instructor below fold) */
    protected void scrollIntoView(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView({block:'center', behavior:'smooth'});", element);
        // Brief pause for smooth scroll to settle - NOT Thread.sleep, just JS execution gap
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
    }

    protected void scrollIntoView(By locator) {
        WebElement element = driver.findElement(locator);
        scrollIntoView(element);
    }

    // ── Element state helpers ──────────────────────────────────────────────

    /**
     * Wait for element to be clickable with explicit wait.
     * Separate from waitForClickable(By) to accept WebElement.
     */
    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Checks if an element exists in the DOM without throwing.
     * Use this before calling getText() or getAttribute() (Edge Case #10, #21).
     */
    protected boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /**
     * Checks visibility of an element that may or may not exist.
     * Returns false instead of throwing NoSuchElementException.
     */
    protected boolean isElementVisible(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get text with stale element retry (Edge Case #9 - post-filter DOM rebuild).
     * Retries up to 3 times before re-throwing.
     */
    protected String getTextWithRetry(By locator) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                return driver.findElement(locator).getText().trim();
            } catch (StaleElementReferenceException e) {
                attempts++;
                logger.warn("StaleElementReferenceException on getText(), attempt {}/3", attempts);
                if (attempts == 3) throw e;
            }
        }
        return "";
    }
}
