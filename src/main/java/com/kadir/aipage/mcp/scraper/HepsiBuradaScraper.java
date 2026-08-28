package com.kadir.aipage.mcp.scraper;

import com.kadir.aipage.dto.ProductPriceInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class HepsiBuradaScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("hepsiburada.com");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        return fetchProductDetailsInternal(productUrl);
    }

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {
        String searchUrl = "https://www.hepsiburada.com/ara?q=" + productName.replace(" ", "+");
        WebDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            driver = new ChromeDriver(options);
            driver.get(searchUrl);

            WebElement firstProduct = driver.findElement(By.cssSelector("li[id^='product-item'] a"));
            String productUrl = firstProduct.getAttribute("href");
            driver.quit();
            return getProductDetails(productUrl);
        } catch (Exception e) {
            if (driver != null) driver.quit();
            return new ProductPriceInfo("Arama Hatası: " + e.getMessage(), null, null, null, "Hepsiburada", searchUrl, 0, 0, 0, 0);
        }
    }

    private ProductPriceInfo fetchProductDetailsInternal(String productUrl) {
        WebDriver driver = null;
        String productName = "Bilinmeyen Hepsiburada Ürünü";
        long driverInitMs = 0, pageLoadMs = 0, titleFindMs = 0, priceFindMs = 0;

        try {
            long stepStart = System.currentTimeMillis();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            driver = new ChromeDriver(options);
            driverInitMs = System.currentTimeMillis() - stepStart;

            stepStart = System.currentTimeMillis();
            try { driver.get(productUrl); } catch (Exception ignored) {}
            pageLoadMs = System.currentTimeMillis() - stepStart;

            stepStart = System.currentTimeMillis();
            try {
                WebElement titleElement = driver.findElement(By.id("product-name"));
                if (titleElement != null) productName = titleElement.getText().trim();
            } catch (Exception ignored) {}
            titleFindMs = System.currentTimeMillis() - stepStart;

            stepStart = System.currentTimeMillis();
            WebElement priceElement = null;
            try {
                priceElement = driver.findElement(By.cssSelector("div[data-test-id='default-price'] span"));
            } catch (Exception ignored) {}
            BigDecimal currentPrice = priceElement != null ? parsePrice(priceElement.getText()) : null;
            priceFindMs = System.currentTimeMillis() - stepStart;

            return new ProductPriceInfo(productName, currentPrice, null, null, "Hepsiburada", productUrl, driverInitMs, pageLoadMs, titleFindMs, priceFindMs);
        } catch (Exception e) {
            return new ProductPriceInfo("Hata: " + e.getMessage(), null, null, null, "Hepsiburada", productUrl, driverInitMs, pageLoadMs, titleFindMs, priceFindMs);
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isEmpty()) return null;
        try {
            String clean = priceText.replaceAll("[^0-9.,]", "");
            if (clean.contains(".") && clean.contains(",")) clean = clean.replace(".", "").replace(",", ".");
            else if (clean.contains(",")) clean = clean.replace(",", ".");
            return new BigDecimal(clean);
        } catch (Exception e) { return null; }
    }
}