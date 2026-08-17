package com.kadir.aipage.mcp.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AmazonScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("amazon.com");
    }

    @Override
    public String getPrice(String productUrl) {
        WebDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();

            // Tarayıcının ekranda açılmasını engeller (Arka planda çalışır)
            options.addArguments("--headless=new");

            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");

            // Bot algılanmasını önleyen kritik ayarlar
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--excludeSwitches=enable-automation");
            options.addArguments("--useAutomationExtension=false");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

            driver = new ChromeDriver(options);


            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );


            if (!productUrl.startsWith("http://") && !productUrl.startsWith("https://")) {
                productUrl = "https://" + productUrl;
            }

            driver.get(productUrl);



            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            WebElement priceElement = null;

            String[] selectors = {
                    ".a-price.apex-core-price-identifier",
                    "#corePriceDisplay_desktop_feature_div .a-price",
                    ".a-price"
            };

            for (String selector : selectors) {
                try {
                    priceElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
                    if (priceElement != null && !priceElement.getText().trim().isEmpty()) {
                        break;
                    }
                } catch (Exception e) {

                }
            }

            if (priceElement != null) {
                String priceText = priceElement.getAttribute("innerText");
                if (priceText == null || priceText.trim().isEmpty()) {
                    priceText = priceElement.getText();
                }

                priceText = priceText.replace("\n", " ").trim();
                return "Amazon Ürün Fiyatı: " + priceText;
            } else {
                return "Amazon otomasyon hatası: Sayfa arkada açıldı ancak fiyat elementleri bulunamadı (Bot korumasına takılmış olabilir).";
            }

        } catch (Exception e) {
            return "Amazon otomasyon hatası: " + e.getMessage();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}