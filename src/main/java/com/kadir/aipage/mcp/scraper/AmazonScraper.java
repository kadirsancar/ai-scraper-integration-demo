package com.kadir.aipage.mcp.scraper;

import com.kadir.aipage.dto.ProductPriceInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Component
public class AmazonScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("amazon.");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        // Mevcut URL tabanlı çalışan metot
        return fetchProductDetailsInternal(productUrl);
    }

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {
        String searchUrl = "https://www.amazon.com.tr/s?k=" + productName.replace(" ", "+");
        WebDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            driver = new ChromeDriver(options);
            driver.get(searchUrl);

            // Arama sonuçlarından ilk ürünün linkini bul
            WebElement firstProduct = driver.findElement(By.cssSelector("div.s-main-slot div[data-component-type='s-search-result'] h2 a"));
            String productUrl = firstProduct.getAttribute("href");

            driver.quit();
            return getProductDetails(productUrl);
        } catch (Exception e) {
            if (driver != null) driver.quit();
            return new ProductPriceInfo("Arama Hatası: " + e.getMessage(), null, null, null, "Amazon", searchUrl, 0, 0, 0, 0);
        }
    }

    private ProductPriceInfo fetchProductDetailsInternal(String productUrl) {
        WebDriver driver = null;
        String productName = "Bilinmeyen Amazon Ürünü";
        long driverInitMs = 0, pageLoadMs = 0, titleFindMs = 0, priceFindMs = 0;

        try {
            long stepStart = System.currentTimeMillis();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driverInitMs = System.currentTimeMillis() - stepStart;

            stepStart = System.currentTimeMillis();
            try { driver.get(productUrl); } catch (Exception ignored) {}
            pageLoadMs = System.currentTimeMillis() - stepStart;

            stepStart = System.currentTimeMillis();
            try {
                WebElement titleElement = driver.findElement(By.id("productTitle"));
                if (titleElement != null) productName = titleElement.getText().trim();
            } catch (Exception ignored) {}
            titleFindMs = System.currentTimeMillis() - stepStart;

            stepStart = System.currentTimeMillis();
            WebElement priceElement = null;
            String[] priceSelectors = {
                    "#corePriceDisplay_desktop_feature_div span.a-price span.a-offscreen",
                    "span.a-price.apex-core-price-identifier span.a-offscreen"
            };
            for (String selector : priceSelectors) {
                try {
                    priceElement = driver.findElement(By.cssSelector(selector));
                    if (priceElement != null && !priceElement.getText().isBlank()) break;
                } catch (Exception ignored) {}
            }

            BigDecimal currentPrice = priceElement != null ? parsePrice(priceElement.getAttribute("textContent")) : null;
            priceFindMs = System.currentTimeMillis() - stepStart;

            return new ProductPriceInfo(productName, currentPrice, null, null, "Amazon", productUrl, driverInitMs, pageLoadMs, titleFindMs, priceFindMs);
        } catch (Exception e) {
            return new ProductPriceInfo("Hata: " + e.getMessage(), null, null, null, "Amazon", productUrl, driverInitMs, pageLoadMs, titleFindMs, priceFindMs);
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank()) return null;
        try {
            String clean = priceText.replaceAll("[^0-9.,]", "").trim();
            if (clean.contains(".") && clean.contains(",")) {
                clean = clean.lastIndexOf(",") > clean.lastIndexOf(".") ? clean.replace(".", "").replace(",", ".") : clean.replace(",", "");
            } else if (clean.contains(",")) {
                clean = clean.replace(",", ".");
            }
            return new BigDecimal(clean);
        } catch (Exception e) { return null; }
    }
}