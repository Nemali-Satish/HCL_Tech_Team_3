package testBase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;

import io.github.bonigarcia.wdm.WebDriverManager;


public class BaseClass {

    // Static so ExtentReportManager can access the same instance
    public static WebDriver driver;
    public Logger logger;
    public Properties properties;

    @BeforeClass(groups = {"Master", "Sanity", "Regression"})
    @Parameters({"os", "browser"})
    public void setup(String os, String browser) throws IOException {

        // ── 1. Load config.properties ──────────────────────────────────────
        FileReader file = new FileReader("./src/test/resources/config.properties");
        properties = new Properties();
        properties.load(file);

        logger = LogManager.getLogger(this.getClass());
        logger.info("=== Test Setup Started | OS: {} | Browser: {} ===", os, browser);

        switch (browser.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver();
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;

            default:
                throw new IllegalArgumentException("Invalid browser: " + browser
                        + ". Valid values: chrome, edge, firefox");
        }

        // ── 3. Browser configuration ───────────────────────────────────────
        driver.manage().deleteAllCookies();

        int pageLoadTimeout = Integer.parseInt(
                properties.getProperty("pageLoadTimeout", "30"));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));

        // ── 4. Navigate and maximise ───────────────────────────────────────

        driver.manage().window().maximize();
        driver.get(properties.getProperty("AppUrl1"));

        logger.info("Browser launched. URL: {}", properties.getProperty("AppUrl1"));
    }

    @AfterClass(groups = {"Master", "Sanity", "Regression"})
    public void tearDown() {
        if (driver != null) {
            logger.info("=== Tearing down browser ===");
            driver.quit();
        }
    }

    // ── Screenshot capture ─────────────────────────────────────────────────

    public String captureScreen(String testName) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        // Ensure screenshots directory exists
        File screenshotsDir = new File(System.getProperty("user.dir") + "/screenshots");
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs();
        }

        TakesScreenshot ts = (TakesScreenshot) driver;
        File sourceFile = ts.getScreenshotAs(OutputType.FILE);

        String targetFilePath = screenshotsDir.getAbsolutePath()
                + "/" + testName + "_" + timeStamp + ".png";
        File targetFile = new File(targetFilePath);

        // FileUtils.copyFile is safer than renameTo across file systems
        FileUtils.copyFile(sourceFile, targetFile);

        logger.info("Screenshot saved: {}", targetFilePath);
        return targetFilePath;
    }

    // ── Config helper ──────────────────────────────────────────────────────
    /**
     * Read integer property with a fallback default.
     * All timeout values come from here - never hardcoded in tests.
     */
    protected int getIntProperty(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null || val.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Config key '{}' is not a valid integer, using default {}", key, defaultValue);
            return defaultValue;
        }
    }
}
