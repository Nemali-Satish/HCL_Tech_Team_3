package testCases;

import org.testng.Assert;
import org.testng.annotations.Test;

import pageObjects.HomePage;
import testBase.BaseClass;

public class TC_001_HomePageTest extends BaseClass {

    // ── TC_01: Valid Search ────────────────────────────────────────────────
    @Test(priority = 1,
          groups = {"Master", "Sanity", "Regression"},
          description = "TC_01 - Open Udemy, handle popups, search valid keyword, verify results URL")
    public void tc01_validSearchKeyword() {
        logger.info("▶ TC_01: Valid search - START");

        int explicit  = getIntProperty("explicitWait", 15);
        int shortWait = getIntProperty("shortWait", 3);

        HomePage homePage = new HomePage(driver, explicit, shortWait);

        // Step 1 & 2: Verify homepage loaded + dismiss popups
        boolean loaded = homePage.isHomePageLoaded();
        Assert.assertTrue(loaded, "Udemy homepage did not load correctly.");

        homePage.handleAllPopups();

        // Step 3: Search for keyword from config (never hardcoded)
        String keyword = properties.getProperty("searchKeyword", "Python");
        homePage.searchForCourse(keyword);

        // Step 4: Verify URL contains the search term (confirms navigation occurred)
        String currentUrl = driver.getCurrentUrl().toLowerCase();
        logger.info("Current URL after search: {}", currentUrl);

        Assert.assertTrue(
                currentUrl.contains(keyword.toLowerCase())
                || currentUrl.contains("search")
                || currentUrl.contains("course"),
                "URL after search did not contain expected search indicator. URL: " + currentUrl
        );

        logger.info("TC_01 PASSED: Search navigation confirmed.");
    }

    // ── TC_02: Empty Search ────────────────────────────────────────────────
    @Test(priority = 2,
          groups = {"Master", "Regression"},
          description = "TC_02 - Empty search box, press Enter - no crash or navigation away")
    public void tc02_emptySearch() {
        logger.info("▶ TC_02: Empty search - START");

        int explicit  = getIntProperty("explicitWait", 15);
        int shortWait = getIntProperty("shortWait", 3);

        // Navigate fresh to homepage for isolation
        driver.get(properties.getProperty("AppUrl1"));

        HomePage homePage = new HomePage(driver, explicit, shortWait);
        homePage.isHomePageLoaded();
        homePage.handleAllPopups();

        // Attempt empty search
        homePage.searchForCourse("");

        // Expected: either stays on homepage OR shows no-results
        // Must NOT crash or show an error page
        String title = driver.getTitle();
        String url   = driver.getCurrentUrl();
        logger.info("After empty search - Title: {}, URL: {}", title, url);

        Assert.assertFalse(
                title.toLowerCase().contains("error") || title.toLowerCase().contains("404"),
                "Empty search caused an error page. Title: " + title
        );

        logger.info("✅ TC_02 PASSED: Empty search did not crash the page.");
    }

    // ── TC_03: Special Characters Search ──────────────────────────────────
    @Test(priority = 3,
          groups = {"Master", "Regression"},
          description = "TC_03 - Special chars input - no crash, graceful no-results or stay on page")
    public void tc03_specialCharactersSearch() {
        logger.info("▶ TC_03: Special characters search - START");

        int explicit  = getIntProperty("explicitWait", 15);
        int shortWait = getIntProperty("shortWait", 3);

        driver.get(properties.getProperty("AppUrl1"));

        HomePage homePage = new HomePage(driver, explicit, shortWait);
        homePage.isHomePageLoaded();
        homePage.handleAllPopups();

        String specialChars = properties.getProperty("specialCharsKeyword", "@#$%");
        homePage.searchForCourse(specialChars);

        String title = driver.getTitle();
        String url   = driver.getCurrentUrl();
        logger.info("After special chars search - Title: {}, URL: {}", title, url);

        // Must NOT crash
        Assert.assertFalse(
                title.toLowerCase().contains("500") || title.toLowerCase().contains("error page"),
                "Special character search caused server error. Title: " + title
        );

        logger.info("TC_03 PASSED: Special chars search handled gracefully.");
    }

    // ── TC_11: Long Input String (Edge Case) ──────────────────────────────
    @Test(priority = 4,
          groups = {"Master", "Regression"},
          description = "TC_11 - Enter 150+ char string - no crash, page remains functional")
    public void tc11_longInputString() {
        logger.info("▶ TC_11: Long input (150 chars) - START");

        int explicit  = getIntProperty("explicitWait", 15);
        int shortWait = getIntProperty("shortWait", 3);

        driver.get(properties.getProperty("AppUrl1"));

        HomePage homePage = new HomePage(driver, explicit, shortWait);
        homePage.isHomePageLoaded();
        homePage.handleAllPopups();

        // Generate 150-character string programmatically (never hardcoded)
        String longInput = "a".repeat(150);
        logger.info("Long input length: {}", longInput.length());

        homePage.searchForCourse(longInput);

        // Page must remain functional - check title or search bar still accessible
        String title = driver.getTitle();
        Assert.assertFalse(
                title.toLowerCase().contains("500") || title.toLowerCase().contains("not found page"),
                "Long input caused server error. Title: " + title
        );

        logger.info("TC_11 PASSED: Long input string handled without crash.");
    }

    
}
