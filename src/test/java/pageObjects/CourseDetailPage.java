package pageObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;


public class CourseDetailPage extends BasePage {

    // ── Locators ───────────────────────────────────────────────────────────
    // Stable selectors - data-purpose and aria-label preferred (Edge Case #25)

    // Course title on detail page
    private static final By COURSE_TITLE
            = By.cssSelector("[data-testid='lead-title'], " +
                             "[class*='udlite-heading-xl'], " +
                             "h1[class*='title'], " +
                             "h1[data-purpose*='lead-title']");

    // Course rating - numeric span
    private static final By COURSE_RATING_NUMERIC
            = By.cssSelector("span[data-purpose='rating-number'], " +
                             "[class*='star-rating-count'], " +
                             "span[class*='review-overview--rating-number']");

    // Course rating via aria-label (Edge Case #17 - star icons, not text)
    private static final By COURSE_RATING_ARIA
            = By.cssSelector("[aria-label*='out of 5'], " +
                             "[aria-label*='Rating:'], " +
                             "[class*='star-rating'][aria-label]");

    // Instructor name - supports multiple instructors (Edge Case #19)
    private static final By INSTRUCTOR_NAME
            = By.cssSelector("[data-purpose='instructor-name-top'], " +
                             "a[class*='instructor-links'], " +
                             "[class*='instructor--instructor__title'], " +
                             "a[href*='/user/'] span");

    // Instructor section wrapper (for scrollIntoView - Edge Case #18)
    private static final By INSTRUCTOR_SECTION
            = By.cssSelector("[data-purpose='instructor-links'], " +
                             "[class*='instructor--instructor'], " +
                             "section[class*='instructor']");

    // Promotional banner close button (Edge Case #20)
    private static final By PROMO_BANNER_CLOSE
            = By.cssSelector("[data-testid='purchase-section-close'], " +
                             "button[aria-label*='close' i][class*='banner'], " +
                             "[class*='promo'] button[aria-label*='dismiss' i]");

    // Page loaded indicator
    private static final By PAGE_LOADED_INDICATOR
            = By.cssSelector("[data-testid='lead-title'], h1[class*='title'], h1");

    // ── State ──────────────────────────────────────────────────────────────
    private String parentWindowHandle;

    // ── Constructor ────────────────────────────────────────────────────────
    public CourseDetailPage(WebDriver driver, int explicitTimeout, int shortTimeout) {
        super(driver, explicitTimeout, shortTimeout);
        // Store parent window BEFORE any click that may open a new tab
        this.parentWindowHandle = driver.getWindowHandle();
    }

    // ── Window/tab handling ────────────────────────────────────────────────

    public void switchToNewTabIfOpened() {
        Set<String> handles = driver.getWindowHandles();
        logger.info("Window handles after card click: {}", handles.size());

        if (handles.size() > 1) {
            for (String handle : handles) {
                if (!handle.equals(parentWindowHandle)) {
                    driver.switchTo().window(handle);
                    logger.info("Switched to new tab. URL: {}", driver.getCurrentUrl());
                    return;
                }
            }
        }
        logger.info("Course opened in same tab. URL: {}", driver.getCurrentUrl());
    }


    public void switchBackToParentWindow() {
        driver.switchTo().window(parentWindowHandle);
        logger.info("Switched back to parent window.");
    }


    public boolean waitForCourseDetailPageToLoad() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(PAGE_LOADED_INDICATOR));
            logger.info("Course detail page loaded. URL: {}", driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            logger.warn("Course detail page did not load within timeout. URL: {}", driver.getCurrentUrl());
            return false;
        }
    }

    // ── Popup/banner handling ──────────────────────────────────────────────

    public void dismissPromoBannerIfPresent() {
        try {
            WebElement closeBtn = shortWaitForVisibility(PROMO_BANNER_CLOSE);
            closeBtn.click();
            logger.info("Promotional banner dismissed.");
        } catch (Exception e) {
            logger.info("No promotional banner found - continuing.");
        }
    }

    // ── Course title ───────────────────────────────────────────────────────


    public String getCourseTitle() {
        try {
            WebElement titleEl = waitForVisibility(COURSE_TITLE);
            String title = titleEl.getText().trim();
            logger.info("Course title found: '{}'", title);
            return title;
        } catch (Exception e) {
            logger.warn("Course title element not found: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Verify course title is visible and non-empty.
     */
    public boolean isTitleVisibleAndNonEmpty() {
        String title = getCourseTitle();
        return title != null && !title.isEmpty();
    }

    // ── Course rating ──────────────────────────────────────────────────────

    public String getCourseRating() {
        // Strategy 1: look for a numeric rating span
        try {
            List<WebElement> numericRatings = driver.findElements(COURSE_RATING_NUMERIC);
            if (!numericRatings.isEmpty()) {
                String rating = numericRatings.get(0).getText().trim();
                if (!rating.isEmpty()) {
                    logger.info("Rating (numeric text): '{}'", rating);
                    return rating;
                }
            }
        } catch (Exception e) {
            logger.debug("Numeric rating getText() failed: {}", e.getMessage());
        }

        try {
            List<WebElement> ariaRatings = driver.findElements(COURSE_RATING_ARIA);
            if (!ariaRatings.isEmpty()) {
                String ariaLabel = ariaRatings.get(0).getAttribute("aria-label");
                if (ariaLabel != null && !ariaLabel.isEmpty()) {
                    logger.info("Rating (aria-label): '{}'", ariaLabel);
                    return ariaLabel;
                }
            }
        } catch (Exception e) {
            logger.debug("aria-label rating fallback failed: {}", e.getMessage());
        }

        logger.warn("No rating element found on course detail page (may be new course).");
        return "";
    }


    public boolean isRatingVisible() {
        String rating = getCourseRating();
        if (rating.isEmpty()) {
            logger.warn("Rating not visible - course may have no reviews yet.");
            return false;
        }
        // Verify it contains a digit (4.5, "4.5 out of 5", etc.)
        boolean hasDigit = rating.matches(".*\\d.*");
        logger.info("Rating '{}' contains numeric value: {}", rating, hasDigit);
        return hasDigit;
    }

    // ── Instructor name ────────────────────────────────────────────────────

    public List<String> getInstructorNames() {
        List<String> names = new ArrayList<>();

        // Scroll to instructor section (Edge Case #18)
        if (isElementPresent(INSTRUCTOR_SECTION)) {
            scrollIntoView(INSTRUCTOR_SECTION);
        } else {
            // Scroll down the page to find instructor section
            js.executeScript("window.scrollBy(0, 600);");
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        try {
            List<WebElement> instructorEls = driver.findElements(INSTRUCTOR_NAME);
            for (WebElement el : instructorEls) {
                String name = el.getText().trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
            logger.info("Instructor names found ({}): {}", names.size(), names);
        } catch (Exception e) {
            logger.warn("Could not retrieve instructor names: {}", e.getMessage());
        }

        return names;
    }


    public String getPrimaryInstructorName() {
        List<String> names = getInstructorNames();
        if (names.isEmpty()) {
            logger.warn("No instructor name found on course detail page.");
            return "";
        }
        return names.get(0);
    }


    public boolean isInstructorNameVisibleAndNonEmpty() {
        String name = getPrimaryInstructorName();
        return name != null && !name.isEmpty();
    }
}
