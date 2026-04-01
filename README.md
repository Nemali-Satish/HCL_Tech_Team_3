# HCL Tech Hackathon — Automation Testing Challenge

> **Udemy — Course Search & Detail Validation**
> Sprint0: Test Plan & Design Document
> **Date:** April 1, 2026

| Property | Value |
|---|---|
| **Framework** | Selenium WebDriver + TestNG + Maven |
| **Language** | Java |
| **Design Pattern** | Page Object Model (POM) |

---

## 1. Problem Statement

This use case validates a guest user journey on Udemy where learners search for online courses and review key details. The automation covers handling dynamic suggestions, result listings, applying filters, and course detail validations — all without enrolling or logging in.

### Automation Tasks

| # | Task | Key Challenge |
|---|---|---|
| 1 | Open https://www.udemy.com | Browser launch, page load wait |
| 2 | Handle cookies/location popups | Dynamic popups, may or may not appear |
| 3 | Search for a generic topic (e.g., Python) | Async search suggestions, dynamic dropdown |
| 4 | Verify course results with multiple cards | Async loading, stale element risk |
| 5 | Apply one filter (rating or level) | Async filter reload, URL change detection |
| 6 | Open first course card | Dynamic card structure, hover states |
| 7 | Verify title, rating, instructor name | New tab handling, element visibility |

### Constraints

- Search suggestions and results load asynchronously — explicit waits mandatory
- No hardcoded course names or prices — all values captured dynamically
- No login or purchase automation
- Use explicit waits for filters and results

---

## 2. Framework Architecture

### Design Pattern: Page Object Model (POM)

The Page Object Model separates test logic from page interaction logic. Each web page gets its own Java class that encapsulates all locators and actions. Test classes only call high-level methods — they never touch a WebElement directly.

**Why POM wins in this hackathon:**

- **Single point of maintenance:** If Udemy changes a CSS selector, we fix ONE file — not every test
- **Readable tests:** `homePage.searchForCourse("Python")` reads like English — perfect for demos
- **Industry standard:** Judges know and expect POM — shows professional thinking

### Layer Architecture

| Layer | Components | Responsibility |
|---|---|---|
| **Test Layer** | HomePageTest, SearchResultsTest, CourseDetailTest | `@Test` methods, assertions, test data, calls page object methods |
| **Page Object Layer** | HomePage, SearchResultsPage, CourseDetailPage | Encapsulates locators (By objects), provides action methods, returns dynamic data |
| **Base Layer** | BasePage, BaseClass | Shared wait/action helpers, WebDriver setup and teardown, config loading |
| **Utilities** | ExtentReportManager, ConfigReader | HTML report generation, screenshot capture, properties file reading |
| **Configuration** | config.properties, log4j2.xml, master.xml | Runtime settings (URL, browser, timeouts), logging config, suite definition |

---

## 3. Project Folder Structure

```
Opencart (Project Root)
├── pom.xml                          # Maven dependencies & build config
├── master.xml                       # TestNG suite definition
├── src/test/java/
│   ├── pageObjects/                 # All Page Object classes
│   │   ├── BasePage.java            # Shared page helpers (waits, clicks)
│   │   ├── HomePage.java            # Udemy homepage interactions
│   │   ├── SearchResultsPage.java   # Search results + filter actions
│   │   └── CourseDetailPage.java    # Course detail validations
│   ├── testBase/                    # Base class for setup/teardown
│   │   └── BaseClass.java           # WebDriver lifecycle management
│   ├── testCases/                   # TestNG test classes
│   │   ├── TC_001_HomePageTest.java         # Open site, handle popups, search
│   │   ├── TC_002_SearchResultsTest.java    # Verify results, apply filter
│   │   └── TC_003_CourseDetailTest.java     # Open course, verify details
│   └── utilities/                   # Reusable utility classes
│       └── ExtentReportManager.java # HTML report generation
├── src/test/resources/
│   ├── config.properties            # AppUrl, browser, timeouts
│   └── log4j2.xml                   # Log4j2 logging configuration
├── testData/                        # External test data files
├── reports/                         # Generated Extent Reports
├── screenshots/                     # Failure screenshots (.png)
├── logs/                            # Text logs (automation.log)
└── target/                          # Compiled files (auto-generated)
```

**Why This Structure?**

- **`pageObjects/`:** Maps 1:1 to Udemy pages — easy to locate, modify, and extend
- **`testBase/`:** Isolates WebDriver lifecycle — every test inherits setup/teardown automatically
- **`testCases/`:** `TC_` prefix with numbering gives clear execution order and traceability
- **`utilities/`:** Cross-cutting concerns separated from page-specific logic
- **`src/test/resources/`:** Config and logging files in Maven's standard resource directory
- **`reports/ + screenshots/ + logs/`:** All output artifacts in dedicated root-level folders — clean for demo

---

## 4. Test Case Design

13 functional test cases (positive and negative) plus 3 edge case tests, organized by feature area. Each test includes its type to show comprehensive coverage.

### 4.1 Search Functionality

#### TC_01 — Valid Search ✅ Positive

| Field | Detail |
|---|---|
| **Scenario** | Search with a valid keyword and verify results are displayed |
| **Precondition** | Udemy homepage is loaded, popups dismissed |
| **Steps** | 1) Open https://www.udemy.com 2) Enter "Python" in search bar (from config.properties) 3) Press Enter |
| **Expected Result** | Results page is displayed with relevant course cards. URL contains the search term. |

#### TC_02 — Empty Search ❌ Negative

| Field | Detail |
|---|---|
| **Scenario** | Submit search with empty input field |
| **Steps** | 1) Open Udemy 2) Leave search box empty 3) Press Enter |
| **Expected Result** | No navigation occurs OR a validation message is shown. No crash or error page. |

#### TC_03 — Special Characters Search ❌ Negative

| Field | Detail |
|---|---|
| **Scenario** | Search with special characters to test input sanitization |
| **Steps** | 1) Open Udemy 2) Enter "@#$%" in search bar 3) Press Enter |
| **Expected Result** | No results shown OR graceful "no results" message. No crash, no error page, no unhandled exception. |

---

### 4.2 Search Results Page

#### TC_04 — Results Display ✅ Positive

| Field | Detail |
|---|---|
| **Scenario** | Verify course list is displayed after a valid search |
| **Steps** | 1) Search "Python" 2) Wait for results to load 3) Count visible course cards |
| **Expected Result** | Course cards are displayed. Count is greater than 0. Each card has a title element. |
| **Technical Approach** | `WebDriverWait` with `numberOfElementsToBeMoreThan(locator, 0)`. Card count captured dynamically, never hardcoded. |

#### TC_05 — No Results for Invalid Keyword ❌ Negative

| Field | Detail |
|---|---|
| **Scenario** | Search with a gibberish keyword that returns no results |
| **Steps** | 1) Search "asdfghjkl123xyz" 2) Wait for page to load |
| **Expected Result** | A "No results found" or "Sorry, we couldn't find any results" message is displayed. Page does not crash. |

---

### 4.3 Filter Functionality

#### TC_06 — Apply Rating Filter ✅ Positive

| Field | Detail |
|---|---|
| **Scenario** | Apply a rating filter (4 stars & above) and verify results update |
| **Steps** | 1) Search "Python" 2) Scroll to filter section 3) Click "4.0 & up" or "4.5 & up" rating filter 4) Wait for results to reload |
| **Expected Result** | Filtered courses are displayed. URL contains filter parameter. Course cards are still present after filtering. |
| **Technical Approach** | `scrollIntoView()` to reach filter section. `stalenessOf(oldElement)` to detect AJAX reload. Re-find results with fresh locators after reload. |

#### TC_07 — Filter with No Results ❌ Negative

| Field | Detail |
|---|---|
| **Scenario** | Apply multiple strict filters that result in zero courses |
| **Steps** | 1) Search a niche topic 2) Apply multiple restrictive filters simultaneously 3) Wait for results reload |
| **Expected Result** | A "no results" message is displayed. Page does not crash. No `StaleElementReferenceException`. |

---

### 4.4 Course Details Page

#### TC_08 — Course Navigation ✅ Positive

| Field | Detail |
|---|---|
| **Scenario** | Click the first course card and verify course detail page opens |
| **Steps** | 1) From search results, capture first card's title 2) Click the first course card 3) Handle new tab if opened (switch window handle) 4) Wait for course detail page to load |
| **Expected Result** | Course detail page opens successfully. Page contains course-specific content (not search results). |
| **Technical Approach** | Store `parentWindow` before click. Check `getWindowHandles().size() > 1`. If new tab, switch to it. If same tab, continue. Handles both scenarios. |

#### TC_09 — Validate Course Information ✅ Positive

| Field | Detail |
|---|---|
| **Scenario** | Verify course title, instructor name, and rating are visible on the detail page |
| **Steps** | 1) Open course detail page 2) Locate course title element 3) Locate instructor name element (scroll if needed) 4) Locate rating element (check aria-label if star icons) |
| **Expected Result** | Course title is visible and non-empty. Instructor name is visible and non-empty. Rating is visible and contains a numeric value. No values are hardcoded. |
| **Technical Approach** | `Assert.assertFalse(title.isEmpty())` for dynamic validation. Use `findElements` (plural) for instructor to handle multiple instructors. Check `aria-label` for star-based ratings. |

#### TC_10 — Missing/Blank Data Validation ❌ Negative

| Field | Detail |
|---|---|
| **Scenario** | Validate that no critical course detail fields are blank or missing |
| **Steps** | 1) Open a course detail page 2) Check course title is not empty/null 3) Check instructor name is not empty/null 4) Check rating section exists |
| **Expected Result** | No blank fields for title or instructor. If rating is missing (new course), the test logs it but does not crash. |
| **Technical Approach** | Use `getText()` and `trim()` to check for empty strings. Use `findElements().size() > 0` to check element existence before accessing it. |

---

### 4.5 Edge Case Tests

#### TC_11 — Long Input String ⚠️ Edge Case

| Field | Detail |
|---|---|
| **Scenario** | Enter a very long search string (100+ characters) in the search bar |
| **Steps** | 1) Open Udemy 2) Enter a 100+ character string in the search bar 3) Press Enter 4) Observe page behavior |
| **Expected Result** | No crash or unhandled exception. Either the input is truncated, or a no-results message appears. Search bar does not break the page layout. |
| **Technical Approach** | Generate a 150-character string programmatically. Use `sendKeys()` to enter it. Verify page remains functional with `titleContains()` or `presenceOfElementLocated()` checks. |

---

### 4.6 Test Case Summary

| Category | ✅ Positive | ❌ Negative | ⚠️ Edge Case | Total |
|---|---|---|---|---|
| Search Functionality | 1 (TC_01) | 2 (TC_02, TC_03) | — | **3** |
| Search Results Page | 1 (TC_04) | 1 (TC_05) | — | **2** |
| Filter Functionality | 1 (TC_06) | 1 (TC_07) | — | **2** |
| Course Details Page | 2 (TC_08, TC_09) | 1 (TC_10) | — | **3** |
| Edge Cases | — | — | 1 (TC_11) | **1** |
| **TOTAL** | **5** | **5** | **1** | **11** |

---

## 5. Key Technical Decisions

| Decision | Chosen | Alternative | Why This Approach |
|---|---|---|---|
| **Wait Strategy** | Explicit Waits (`WebDriverWait` + `ExpectedConditions`) | Implicit Waits (`driver.manage().timeouts()`) | Problem statement requires explicit waits. Implicit waits apply globally causing unpredictable timeouts. Mixing both causes doubled timeout bugs — a known Selenium anti-pattern. |
| **Design Pattern** | Page Object Model with BasePage inheritance | Procedural scripts (all code in test methods) | Reusability, maintainability, readability. Tests read like user stories for demo. Industry standard that judges expect. |
| **Locator Strategy** | CSS Selectors (primary), XPath (fallback) | XPath for everything | CSS selectors are faster (browser-native engine), more readable, and shorter. XPath only needed for text-based lookups or ancestor traversal. |
| **Test Data** | `config.properties` with `ConfigReader` | Hardcoded values in test classes | Problem statement says avoid hardcoded values. Changing search term requires zero code changes. Demonstrates data-driven thinking. |
| **Reporting** | ExtentReports HTML | Allure Reports | Single HTML file — easy to show in demo. Beautiful pass/fail charts. Screenshot embedding on failure. Faster setup than Allure (no CLI tools). |
| **Logging** | Log4j2 with `log4j2.xml` | `System.out.println()` or `java.util.logging` | Configurable log levels (INFO, DEBUG, ERROR). File + console output. Industry standard. Shows professional practice to judges. |

---

## 6. Risk Mitigation

| Risk | Impact | Mitigation Strategy |
|---|---|---|
| Udemy UI changes during hackathon | **HIGH** | Use stable attributes (`data-testid`, `aria-label`). Avoid fragile absolute XPaths. POM ensures single-file fix. |
| Async loading causes flaky tests | **HIGH** | Explicit waits with 10–15s timeouts. Use `stalenessOf()` for reload detection. Retry logic in BaseClass. |
| Cookie/popup inconsistency | **MEDIUM** | Try-catch with short timeout (3s). Don't fail test if popup is absent. Log whether popup was found. |
| New tab opens on course click | **MEDIUM** | Check window handles count after click. Switch to new tab if handles > 1. Store parent handle for switching back. |
| Network latency at venue | **LOW** | Configurable timeout in `config.properties`. Can increase on the fly without recompiling. |

---

## 7. Edge Cases & Handling Strategies

### 7.1 Task 1–2: Open Udemy & Handle Popups

| # | Edge Case | What Happens | Handling Strategy |
|---|---|---|---|
| 1 | Cookie banner may not appear | No cookie consent dialog is shown. Test hangs waiting for a non-existent element and times out. | Use try-catch with short `WebDriverWait` (3–5 seconds). Catch `TimeoutException` silently. Never fail the test for a missing popup. |
| 2 | Location popup may not appear | Location/language selection dialog doesn't show based on IP or cookies. | Same try-catch pattern as cookie banner. Handle independently — one popup's absence should not affect the other. |
| 3 | Multiple overlapping popups | Both cookie banner AND location popup appear simultaneously, blocking each other. | Handle sequentially: dismiss cookie first, add brief wait (1s), then dismiss location. Each in its own try-catch block. |
| 4 | Page title differs from expected | Udemy shows "Online Courses - Learn Anything" instead of just "Udemy". | Use `titleContains()` instead of `titleIs()`. Check for partial match like "Udemy" or "Online Courses". |

### 7.2 Task 3: Search for a Course Topic

| # | Edge Case | What Happens | Handling Strategy |
|---|---|---|---|
| 5 | Suggestions dropdown never loads | AJAX autocomplete fails due to slow network. Test waits indefinitely for suggestions. | Do NOT depend on suggestions appearing. Type search term and press `Keys.ENTER` directly. Treat autocomplete as optional. |
| 6 | Search input not immediately interactable | Page is loading or responsive layout hasn't rendered the search bar yet. | Use `WebDriverWait` with `elementToBeClickable()` instead of just `visibilityOf()`. Clickable ensures the element is both visible AND enabled. |
| 7 | Autocomplete covers search button | Suggestions dropdown overlays the search button. Clicking button clicks a suggestion instead. | Use `Keys.ENTER` on the search input field instead of clicking the search button. This bypasses any overlay completely. |
| 8 | Overlay/banner blocks search input | Undismissed popup sits on top of search bar. Regular click throws `ElementClickInterceptedException`. | Always handle popups before search. If click still fails, use `JavascriptExecutor` click as fallback: `((JavascriptExecutor)driver).executeScript("arguments[0].click()", element)`. |

### 7.3 Task 4–5: Results Page & Filters

| # | Edge Case | What Happens | Handling Strategy |
|---|---|---|---|
| 9 | `StaleElementReferenceException` after filter | Filter triggers AJAX reload. Old `WebElement` references point to destroyed DOM nodes. | After clicking filter, use `WebDriverWait` with `stalenessOf(oldElement)` to wait for old results to disappear. Then re-find new results with fresh locators. |
| 10 | Zero results after filtering | Strict filter returns no courses. Assertion on card count fails. | Check for "no results" message element before counting cards. If no-results message is present, log it and skip card assertions gracefully. |
| 11 | Lazy-loaded cards (infinite scroll) | Only initial batch of cards is loaded. Total count depends on scroll position. | Never assert exact card count. Use `numberOfElementsToBeMoreThan(locator, 0)` to verify at least some cards are present. |
| 12 | Filter section requires scrolling | Rating/level filters are below the visible viewport. Click fails or hits wrong element. | Scroll to filter element before clicking: `((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true)", filterElement)`. Add brief wait after scroll. |
| 13 | Sponsored/promoted cards in results | Ad cards have different HTML structure than organic course cards. Locator mismatch. | Use a locator that matches the common course card container element, not one specific to organic or sponsored results. |
| 14 | Loading spinner never disappears | After applying filter, skeleton/spinner stays due to slow network. Results never appear within timeout. | Set reasonable timeout (15s). Use `invisibilityOf(spinner)` or `presenceOfElementLocated(courseCard)`. Add clear failure message: "Results did not load within timeout." |

### 7.4 Task 6–7: Open Course & Verify Details

| # | Edge Case | What Happens | Handling Strategy |
|---|---|---|---|
| 15 | Course opens in a new tab | Clicking course card opens new browser tab. All assertions run on old tab (results page) and fail. | Store `parentWindow = driver.getWindowHandle()` before click. After click, check `getWindowHandles().size() > 1`. If yes, switch to the new handle. |
| 16 | Course opens in same tab | Course detail loads in current tab. Window switch logic tries to switch but there's no new tab. | Check window handles count. If only 1 handle exists, skip the switch. Handle both scenarios — never assume one or the other. |
| 17 | Rating displayed as star icons, not text | Rating is rendered as SVG stars or CSS icons. `getText()` returns empty string. | Look for numeric rating in `aria-label` attribute (e.g., `aria-label="4.6 out of 5"`) or a nearby `<span>`. Use `getAttribute("aria-label")` as fallback. |
| 18 | Instructor name below the fold | Instructor section is lower on the page. `isDisplayed()` returns false because it's not in viewport. | Scroll to instructor element with `scrollIntoView()` before asserting visibility. Use `WebDriverWait` after scroll. |
| 19 | Multiple instructors listed | Course has 2–3 instructors. Assertion for exactly one instructor name fails. | Use `findElements` (plural) and check `list.size() >= 1`. Assert the first instructor's name is non-empty. Never hardcode instructor count. |
| 20 | Promotional banner covers content | Udemy shows "Sale ends today!" banner overlaying course details. Click/read fails. | Try to dismiss promotional banner (close button) in a try-catch. If no close button, scroll past the banner to reach target content. |
| 21 | Course card has no rating | New course with zero reviews. Rating element doesn't exist on detail page. Assertion crashes. | On results page, verify the first card has a visible rating element before clicking. If not, iterate to the next card that has a rating. Use a loop with `findElements().size() > 0` check. |
| 22 | Hover tooltip blocks click target | Mouse hover on course card triggers a popover tooltip that covers the click target element. | Use `Actions` class to move to element and click in one motion. Or use `JavascriptExecutor` click as fallback to bypass any overlay. |

> **Total edge cases identified: 22**
>

## 8. Tools & Dependencies

| Tool/Library | Version | Purpose |
|---|---|---|
| **Java JDK** | 17+ | Programming language |
| **Selenium WebDriver** | 4.x | Browser automation engine |
| **TestNG** | 7.x | Test framework (annotations, assertions, suite management) |
| **Maven** | 3.x | Build tool, dependency management |
| **ExtentReports** | 5.x | HTML test reporting with charts and screenshots |
| **Log4j2** | 2.x | Configurable logging (file + console output) |
| **WebDriverManager** | 5.x | Automatic browser driver management (no manual downloads) |
| **Eclipse IDE** | Latest | Development environment |
| **Google Chrome** | Latest | Target browser for test execution |
