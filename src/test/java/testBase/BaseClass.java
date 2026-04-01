package testBase;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BaseClass {
	
	// Made static so the ExtentReportManager can access the same driver instance
	public static WebDriver driver; 
	public Logger logger;
	public Properties properties;
	
	@BeforeClass(groups = {"Master","Sanity","Regression"})
	@Parameters({"os","browser"})
	public void setup(String os, String browser) throws IOException {
		
		// Loading config.properties
		FileReader file = new FileReader("./src/test/resources/config.properties");
		properties = new Properties();
		properties.load(file);
		
		logger = LogManager.getLogger(this.getClass());
		
		switch (browser.toLowerCase()) {
			case "chrome": driver = new ChromeDriver(); break;
			case "edge": driver = new EdgeDriver(); break;
			case "firefox": driver = new FirefoxDriver(); break;
			default:
				System.out.println("Invalid Browser Name");
				return;
		}
		
		driver.manage().deleteAllCookies();
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		
		driver.get(properties.getProperty("AppUrl1"));
		driver.manage().window().maximize();
	}
	
	@AfterClass(groups = {"Master","Sanity","Regression"})
	public void tearDown() {
		driver.quit();
	}
	
	// --- Added captureScreen Method ---
	public String captureScreen(String tname) throws IOException {
		String timeStamp = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
				
		TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
		File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
		
		// Ensure you have a folder named 'screenshots' in your project root
		String targetFilePath = System.getProperty("user.dir") + "\\screenshots\\" + tname + "_" + timeStamp + ".png";
		File targetFile = new File(targetFilePath);
		
		sourceFile.renameTo(targetFile);
			
		return targetFilePath;
	}
	
	// --- Utility methods for random data ---
	public String randomString() {
		return RandomStringUtils.randomAlphabetic(5);
	}
	
	public String randomNumbers() {
		return RandomStringUtils.randomNumeric(10);
	}
	
	public String randomAlphaNumeric() {
		return RandomStringUtils.randomAlphanumeric(10);
	}
}