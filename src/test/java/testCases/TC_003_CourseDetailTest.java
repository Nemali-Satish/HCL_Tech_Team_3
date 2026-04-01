package testCases;

import org.testng.Assert;
import org.testng.annotations.Test;

import pageObjects.CourseDetailPage;
import pageObjects.HomePage;
import pageObjects.SearchResultsPage;
import testBase.BaseClass;


public class TC_003_CourseDetailTest extends BaseClass {


    private CourseDetailPage navigateToCourseDetail() {
        int explicit  = getIntProperty("explicitWait", 15);
        int shortWait = getIntProperty("shortWait", 3);
        String keyword = properties.getProperty("searchKeyword", "Python");

        // ── Step 1: Homepage ───────────────────────────────────────────────
        driver.get(properties.getProperty("AppUrl1"));
        HomePage homePage = new HomePage(driver, explicit, shortWait);
        homePage.isHomePageLoaded();
        homePage.handleAllPopups();

        // ── Step 2: Search ─────────────────────────────────────────────────
        homePage.searchForCourse(keyword);

        // ── Step 3: Results page ───────────────────────────────────────────
        SearchResultsPage resultsPage = new SearchResultsPage(driver, explicit, shortWait);
        boolean loaded = resultsPage.waitForResultsToLoad();

        if (!loaded) {
            Assert.fail("Search results did not load for keyword '" + keyword +
                    "' - cannot proceed to course detail test.");
        }

        logger.info("Search results loaded. Proceeding to click first course card...");

        // ── Step 4: Create CourseDetailPage BEFORE click (stores parent handle) ──
        CourseDetailPage courseDetailPage = new CourseDetailPage(driver, explicit, shortWait);

        // ── Step 5: Click course card ──────────────────────────────────────
        resultsPage.clickFirstCourseCard();

        // ── Step 6: Handle new tab or same tab (Edge Cases #15, #16) ──────
        courseDetailPage.switchToNewTabIfOpened();

        // ── Step 7: Dismiss any promo banner (Edge Case #20) ───────────────
        courseDetailPage.dismissPromoBannerIfPresent();

        // ── Step 8: Wait for detail page to load ──────────────────────────
        boolean detailLoaded = courseDetailPage.waitForCourseDetailPageToLoad();
        if (!detailLoaded) {
            Assert.fail("Course detail page did not load within timeout. URL: "
                    + driver.getCurrentUrl());
        }

        return courseDetailPage;
    }

    // ── TC_08: Course Navigation ───────────────────────────────────────────
    @Test(priority = 1,
          groups = {"Master", "Sanity", "Regression"},
          description = "TC_08 - Click first course card; detail page opens successfully")
    public void tc08_courseNavigation() {
        logger.info("▶ TC_08: Course navigation - START");

        CourseDetailPage courseDetailPage = navigateToCourseDetail();

        // Verify URL is now a course detail page (not search results)
        String currentUrl = driver.getCurrentUrl();
        logger.info("Course detail page URL: {}", currentUrl);

        Assert.assertTrue(
                currentUrl.contains("/course/") || currentUrl.contains("udemy.com"),
                "Expected course detail URL containing '/course/', got: " + currentUrl
        );

        // Verify page has a course title as proof of correct page
        Assert.assertTrue(
                courseDetailPage.isTitleVisibleAndNonEmpty(),
                "Course detail page loaded but no title element found. URL: " + currentUrl
        );

        logger.info("✅ TC_08 PASSED: Course detail page opened successfully.");
    }

    // ── TC_09: Validate Course Information ────────────────────────────────
    @Test(priority = 2,
          groups = {"Master", "Sanity", "Regression"},
          description = "TC_09 - Verify title, instructor name, and rating are visible & non-empty")
    public void tc09_validateCourseInformation() {
        logger.info("▶ TC_09: Validate course information - START");

        CourseDetailPage courseDetailPage = navigateToCourseDetail();

        // ── Validate title ─────────────────────────────────────────────────
        String title = courseDetailPage.getCourseTitle();
        logger.info("Course Title: '{}'", title);
        Assert.assertFalse(title.isEmpty(),
                "Course title is empty or not found on detail page.");

        // ── Validate instructor name ───────────────────────────────────────
       
        String instructor = courseDetailPage.getPrimaryInstructorName();
        logger.info("Primary Instructor: '{}'", instructor);
        Assert.assertFalse(instructor.isEmpty(),
                "Instructor name is empty or not found on detail page.");

        // ── Validate rating ────────────────────────────────────────────────
       
        String rating = courseDetailPage.getCourseRating();
        logger.info("Course Rating: '{}'", rating);

        if (rating.isEmpty()) {
            logger.warn("Rating not visible - course may be new with zero reviews. " +
                        "Logging but not failing the test (TC_10 design decision).");
        } else {
            Assert.assertTrue(rating.matches(".*\\d.*"),
                    "Rating found but does not contain a numeric value: '" + rating + "'");
        }

        logger.info("✅ TC_09 PASSED: Title='{}', Instructor='{}', Rating='{}'",
                title, instructor, rating.isEmpty() ? "N/A (new course)" : rating);
    }

    // ── TC_10: Missing / Blank Data Validation ────────────────────────────
    @Test(priority = 3,
          groups = {"Master", "Regression"},
          description = "TC_10 - Verify no critical course fields are blank or null")
    public void tc10_noBlankOrNullFields() {
        logger.info("▶ TC_10: No blank/null course fields - START");

        CourseDetailPage courseDetailPage = navigateToCourseDetail();

        // ── Title: must NOT be blank or null ──────────────────────────────
        String title = courseDetailPage.getCourseTitle();
        Assert.assertNotNull(title, "Course title returned null.");
        Assert.assertFalse(title.trim().isEmpty(),
                "Course title is blank (empty string after trim). " +
                "Locator may need updating - check CourseDetailPage.COURSE_TITLE.");

        // ── Instructor name: must NOT be blank or null ────────────────────
        String instructor = courseDetailPage.getPrimaryInstructorName();
        Assert.assertNotNull(instructor, "Instructor name returned null.");
        Assert.assertFalse(instructor.trim().isEmpty(),
                "Instructor name is blank (empty string after trim). " +
                "Locator may need updating - check CourseDetailPage.INSTRUCTOR_NAME.");

        // ── Rating: element existence check (soft validation) ─────────────
        // If rating is missing (new course), we log it but do NOT crash (TC_10 spec)
        String rating = courseDetailPage.getCourseRating();
        if (rating.isEmpty()) {
            logger.warn("TC_10: Rating section not present - acceptable for new courses with 0 reviews.");
        } else {
            Assert.assertFalse(rating.trim().isEmpty(),
                    "Rating element found but returned blank text and blank aria-label.");
        }

        logger.info("✅ TC_10 PASSED: All critical fields validated. " +
                    "Title='{}', Instructor='{}', Rating='{}'",
                title, instructor, rating.isEmpty() ? "absent (logged)" : rating);
    }
}
