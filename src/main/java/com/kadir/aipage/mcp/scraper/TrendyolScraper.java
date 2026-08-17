package com.kadir.aipage.mcp.scraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TrendyolScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("trendyol.com");
    }

    @Override
    public String getPrice(String productUrl) {
        WebDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();



            options.addArguments("--headless=new");


            options.addArguments("--window-size=1920,1080"); // Büyük bir ekran boyutu taklidi
            options.addArguments("--disable-blink-features=AutomationControlled"); // Selenium imzasını gizler
            options.addArguments("--excludeSwitches=enable-automation"); // "Otomasyon yazılımı tarafından kontrol ediliyor" uyarısını gizler
            options.addArguments("--useAutomationExtension=false"); // Otomasyon uzantısını devre dışı bırakır
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");

            driver = new ChromeDriver(options);
            driver.get(productUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            WebElement priceElement = null;


            String[] selectors = {
                    ".price-view .discounted",
                    "div.price-wrapper div.dsc",
                    ".prc-dsc",
                    "span[class*='discounted']"
            };

            for (String selector : selectors) {
                try {
                    priceElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector)));
                    if (priceElement != null && !priceElement.getText().trim().isEmpty()) {
                        System.out.println("Trendyol fiyatı şu seçici ile bulundu: " + selector);
                        break;
                    }
                } catch (Exception e) {

                }
            }

            if (priceElement != null) {
                String priceText = priceElement.getText();
                return "Trendyol Ürün Fiyatı: " + priceText.trim();
            } else {
                return "Trendyol otomasyon hatası: Sayfa açıldı ancak fiyat elementleri hiçbir seçici ile bulunamadı.";
            }

        } catch (Exception e) {
            return "Trendyol otomasyon hatası: " + e.getMessage();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}