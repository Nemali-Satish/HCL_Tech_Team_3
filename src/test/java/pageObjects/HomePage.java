package pageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;


public class HomePage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────
    // Using CSS selectors (primary) and data-purpose attributes for stability (Edge Case #25)
    // These are the most stable attributes on Udemy as of April 2026

    // Cookie consent banner
    private static final By COOKIE_BANNER_CLOSE
            = By.cssSelector("[data-testid='close-button'], button[aria-label*='cookie' i], " +
                             "button[aria-label*='accept' i], #onetrust-accept-btn-handler");

    // Location / language popup
    private static final By LOCATION_POPUP_CLOSE
            = By.cssSelector("[data-testid='close-modal'], button[aria-label*='close' i], " +
                             "button[data-dismiss='modal'], " +
                             "div[class*='modal'] button[aria-label*='close' i]");

    // Search bar - most stable selector on Udemy
    private static final By SEARCH_INPUT
            = By.cssSelector("input[data-testid='search-input'], " +
                             "input[placeholder*='Search for anything' i], " +
                             "input[name='q']");

    // Search button (fallback if Enter doesn't work)
    private static final By SEARCH_BUTTON
            = By.cssSelector("button[data-testid='search-button'], " +
                             "button[aria-label*='search' i], " +
                             "form[role='search'] button[type='submit']");

    // ── Constructor ────────────────────────────────────────────────────────
    public HomePage(WebDriver driver, int explicitTimeout, int shortTimeout) {
        super(driver, explicitTimeout, shortTimeout);
    }

    // ── Page state verifications ───────────────────────────────────────────


    public boolean isHomePageLoaded() {
        try {
            wait.until(ExpectedConditions.titleContains("Udemy"));
            logger.info("Homepage loaded. Title: {}", driver.getTitle());
            return true;
        } catch (Exception e) {
            logger.warn("Homepage title did not contain 'Udemy'. Actual: {}", driver.getTitle());
            // Fallback: check if search bar is present
            return isElementPresent(SEARCH_INPUT);
        }
    }

    // ── Popup handling ─────────────────────────────────────────────────────


    public void dismissCookieBannerIfPresent() {
        try {
            WebElement closeBtn = shortWaitForVisibility(COOKIE_BANNER_CLOSE);
            closeBtn.click();
            logger.info("Cookie banner dismissed.");
        } catch (Exception e) {
            logger.info("Cookie banner not present (or already dismissed) - continuing.");
        }
    }


    public void dismissLocationPopupIfPresent() {
        // Brief pause between popup dismissals (Edge Case #3 - overlapping popups)
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        try {
            WebElement closeBtn = shortWaitForVisibility(LOCATION_POPUP_CLOSE);
            closeBtn.click();
            logger.info("Location/language popup dismissed.");
        } catch (Exception e) {
            logger.info("Location popup not present - continuing.");
        }
    }


    public void handleAllPopups() {
        dismissCookieBannerIfPresent();
        dismissLocationPopupIfPresent();
        logger.info("Popup handling complete.");
    }

    // ── Search actions ─────────────────────────────────────────────────────

  
    public void searchForCourse(String keyword) {
        logger.info("Searching for: '{}'", keyword);

       
        WebElement searchInput = waitForClickable(SEARCH_INPUT);
        safeClick(searchInput);

      
        searchInput.clear();
        searchInput.sendKeys(keyword);

       
        searchInput.sendKeys(Keys.ENTER);

        logger.info("Search submitted for: '{}'", keyword);
    }

 
    public String getPageTitle() {
        return driver.getTitle();
    }
}
