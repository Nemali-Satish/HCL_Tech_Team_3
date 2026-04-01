package pageObjects;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;


public class SearchResultsPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────

    // Course card container - matches both organic and sponsored (Edge Case #13)
    private static final By COURSE_CARDS
            = By.cssSelector("div[data-testid='course-card'], " +
                             "div[class*='course-card--container'], " +
                             "[data-purpose='course-card-title-link']");

    // Course card title within each card
    public static final By COURSE_CARD_TITLE
            = By.cssSelector("[data-testid='course-card-title'], " +
                             "[class*='course-title'], " +
                             "h3[class*='title']");

    // Course card rating (for pre-click validation - Edge Case #21)
    public static final By COURSE_CARD_RATING
            = By.cssSelector("[data-testid='course-card-rating'], " +
                             "span[data-purpose='rating-number'], " +
                             "[class*='star-rating']");

    // No results message
    private static final By NO_RESULTS_MESSAGE
            = By.cssSelector("[data-testid='no-results'], " +
                             "[class*='no-results'], " +
                             "p[class*='sorry']");

    // Loading spinner / skeleton (Edge Case #14)
    private static final By LOADING_SPINNER
            = By.cssSelector("[class*='loading-overlay'], " +
                             "[class*='skeleton-loader'], " +
                             "[aria-label='Loading']");

    // Rating filter: "4.5 & up" or "4.0 & up"
    private static final By RATING_FILTER_45
            = By.cssSelector("[data-testid*='rating-45'], " +
                             "label[for*='4.5'], " +
                             "input[aria-label*='4.5 & up' i]");

    private static final By RATING_FILTER_40
            = By.cssSelector("label[for*='4.0'], " +
                             "input[aria-label*='4.0 & up' i], " +
                             "span[data-purpose*='rating-filter'] ~ span");

    // Level filter: Beginner
    private static final By LEVEL_FILTER_BEGINNER
            = By.cssSelector("label[for*='beginner' i], " +
                             "input[aria-label*='beginner' i], " +
                             "[data-testid*='level-beginner']");

    // Filter section header (used for scroll anchor - Edge Case #12)
    private static final By FILTER_SECTION
            = By.cssSelector("[data-testid='filter-section'], " +
                             "[class*='sidebar-filter'], " +
                             "aside[class*='filter']");

    // ── Constructor ────────────────────────────────────────────────────────
    public SearchResultsPage(WebDriver driver, int explicitTimeout, int shortTimeout) {
        super(driver, explicitTimeout, shortTimeout);
    }

    // ── Results verification ───────────────────────────────────────────────


    public boolean waitForResultsToLoad() {
        try {
            // First wait for any spinner to disappear (Edge Case #14)
            if (isElementPresent(LOADING_SPINNER)) {
                waitForInvisibility(LOADING_SPINNER);
            }
            // Then wait for at least 1 course card
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(COURSE_CARDS, 0));
            logger.info("Search results loaded. Card count: {}", getCourseCardCount());
            return true;
        } catch (Exception e) {
            logger.warn("Results did not load within timeout. Checking for no-results message.");
            return false;
        }
    }


    public int getCourseCardCount() {
        try {
            return driver.findElements(COURSE_CARDS).size();
        } catch (Exception e) {
            logger.warn("Could not count course cards: {}", e.getMessage());
            return 0;
        }
    }


    public boolean isNoResultsMessageDisplayed() {
        boolean present = isElementPresent(NO_RESULTS_MESSAGE);
        if (present) logger.info("No results message detected on page.");
        return present;
    }


    public boolean hasVisibleCourseCards() {
        List<WebElement> cards = driver.findElements(COURSE_CARDS);
        if (cards.isEmpty()) {
            logger.warn("No course cards found on results page.");
            return false;
        }
        // Verify at least the first card has a title element
        boolean firstCardHasTitle = !driver.findElements(COURSE_CARD_TITLE).isEmpty();
        logger.info("Cards found: {}. First card has title: {}", cards.size(), firstCardHasTitle);
        return firstCardHasTitle;
    }

    // ── Filter actions ─────────────────────────────────────────────────────


    public boolean applyRatingFilter() {
        logger.info("Attempting to apply rating filter...");

        // Capture reference to an existing card to detect staleness after filter reload
        WebElement cardBeforeFilter = null;
        try {
            List<WebElement> cards = driver.findElements(COURSE_CARDS);
            if (!cards.isEmpty()) cardBeforeFilter = cards.get(0);
        } catch (Exception ignored) {}

        // Scroll to filter section (Edge Case #12)
        if (isElementPresent(FILTER_SECTION)) {
            scrollIntoView(FILTER_SECTION);
        }

        // Try 4.5-star first, fall back to 4.0-star
        boolean clicked = tryClickFilter(RATING_FILTER_45, "4.5 stars");
        if (!clicked) {
            clicked = tryClickFilter(RATING_FILTER_40, "4.0 stars");
        }

        if (!clicked) {
            logger.warn("Rating filter not found - may not be displayed for this search.");
            return false;
        }


        if (cardBeforeFilter != null) {
            try {
                wait.until(ExpectedConditions.stalenessOf(cardBeforeFilter));
                logger.info("Old results became stale - filter AJAX reload confirmed.");
            } catch (Exception e) {
                logger.info("Staleness not detected - results may have reloaded in-place.");
            }
        }

        // Re-find fresh results after reload (Edge Case #9 - never reuse stale references)
        return waitForResultsToLoad();
    }


    public boolean applyBeginnerLevelFilter() {
        logger.info("Attempting to apply Beginner level filter...");

        if (isElementPresent(FILTER_SECTION)) {
            scrollIntoView(FILTER_SECTION);
        }

        WebElement cardBeforeFilter = null;
        try {
            List<WebElement> cards = driver.findElements(COURSE_CARDS);
            if (!cards.isEmpty()) cardBeforeFilter = cards.get(0);
        } catch (Exception ignored) {}

        boolean clicked = tryClickFilter(LEVEL_FILTER_BEGINNER, "Beginner level");
        if (!clicked) {
            logger.warn("Beginner level filter not found.");
            return false;
        }

        if (cardBeforeFilter != null) {
            try {
                wait.until(ExpectedConditions.stalenessOf(cardBeforeFilter));
            } catch (Exception e) {
                logger.info("Staleness not detected after level filter.");
            }
        }

        return waitForResultsToLoad();
    }


    public WebElement getFirstRatedCourseCard() {
        List<WebElement> cards = driver.findElements(COURSE_CARDS);
        logger.info("Scanning {} cards for one with a visible rating...", cards.size());

        for (int i = 0; i < Math.min(cards.size(), 10); i++) {
            try {
                WebElement card = cards.get(i);
                // Check if this card has a rating element (Edge Case #21)
                List<WebElement> ratings = card.findElements(
                        By.cssSelector("span[data-purpose='rating-number'], " +
                                       "[class*='star-rating'], " +
                                       "span[class*='rating']"));
                if (!ratings.isEmpty()) {
                    logger.info("Found rated course at index {}.", i);
                    return card;
                }
            } catch (StaleElementReferenceException e) {
                logger.warn("StaleElement at card index {}, skipping.", i);
            }
        }
        logger.warn("No rated course card found in first 10 results.");
        return null;
    }


    public String getFirstCourseCardTitle() {
        try {
            List<WebElement> titles = driver.findElements(COURSE_CARD_TITLE);
            if (!titles.isEmpty()) {
                String title = titles.get(0).getText().trim();
                logger.info("First course card title: '{}'", title);
                return title;
            }
        } catch (StaleElementReferenceException e) {
            logger.warn("StaleElement reading first card title.");
        }
        return "";
    }

    public void clickFirstCourseCard() {
        WebElement targetCard = getFirstRatedCourseCard();

        if (targetCard == null) {
            logger.warn("No rated card found; clicking first available card instead.");
            List<WebElement> allCards = driver.findElements(COURSE_CARDS);
            if (allCards.isEmpty()) throw new RuntimeException("No course cards present on results page.");
            targetCard = allCards.get(0);
        }

        logger.info("Clicking course card...");
        safeClick(targetCard);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private boolean tryClickFilter(By filterLocator, String filterName) {
        try {
            if (isElementPresent(filterLocator)) {
                scrollIntoView(filterLocator);
                safeClick(filterLocator);
                logger.info("Clicked filter: {}", filterName);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Could not click filter '{}': {}", filterName, e.getMessage());
        }
        return false;
    }
}
