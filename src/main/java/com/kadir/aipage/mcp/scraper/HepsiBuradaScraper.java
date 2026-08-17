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
public class HepsiBuradaScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("hepsiburada.com");
    }

    @Override
    public String getPrice(String productUrl) {
        WebDriver driver = null;

        try {
            System.out.println("HepsiBuradaScraper devrede, URL: " + productUrl);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

            driver = new ChromeDriver(options);
            driver.get(productUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(19));

            WebElement priceElement = null;



            String[] selectors = {
                    "[data-test-id='default-price']",
                    "[data-test-id='price']",
                    "[data-test-id='price-current-price']",
                    "div[data-test-id='price-info'] span",
                    "#offering-price"
            };

            for (String selector : selectors) {
                try {
                    priceElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
                    if (priceElement != null && !priceElement.getText().trim().isEmpty()) {
                        break; // Fiyat bulunduysa döngüden çık
                    }
                } catch (Exception e) {

                }
            }


            if (priceElement == null) {
                try {
                    priceElement = driver.findElement(By.xpath("//*[ (contains(text(), 'TL') or contains(text(), '₺')) and string-length(text()) < 20 ]"));
                } catch (Exception e) {
                    // Bulunamadı
                }
            }

            if (priceElement != null) {
                String priceText = priceElement.getText();
                if (priceText == null || priceText.trim().isEmpty()) {
                    priceText = priceElement.getAttribute("innerText");
                }


                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2})?\\s*TL)");
                java.util.regex.Matcher matcher = pattern.matcher(priceText);

                if (matcher.find()) {
                    priceText = matcher.group(1);
                }

                return "Ürün Güncel Fiyatı: " + priceText.trim();
            } else {
                return "Sayfa yüklendi ancak Hepsiburada fiyat elementi hiçbir seçici ile bulunamadı.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Hepsiburada otomasyon hatası: " + e.getMessage();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}