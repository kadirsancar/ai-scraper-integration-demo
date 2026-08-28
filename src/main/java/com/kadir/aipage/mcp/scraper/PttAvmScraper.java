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

@Component
public class PttAvmScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("pttavm.com");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        return fetchProductDetailsInternal(productUrl);
    }

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {
        String searchUrl = "https://www.pttavm.com/arama?q=" + productName.replace(" ", "+");
        WebDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            driver = new ChromeDriver(options);
            driver.get(searchUrl);

            WebElement firstProduct = driver.findElement(By.cssSelector("a.product-item, div.product-card a"));
            String productUrl = firstProduct.getAttribute("href");
            driver.quit();
            return getProductDetails(productUrl);
        } catch (Exception e) {
            if (driver != null) driver.quit();
            return new ProductPriceInfo("Arama Hatası: " + e.getMessage(), null, null, null, "PttAvm", searchUrl, 0, 0, 0, 0);
        }
    }

    private ProductPriceInfo fetchProductDetailsInternal(String productUrl) {
        WebDriver driver = null;
        String productName = "Bilinmeyen PttAvm Ürünü";
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
                WebElement titleElement = driver.findElement(By.tagName("h1"));
                if (titleElement != null) productName = titleElement.getText().trim();
            } catch (Exception ignored) {}
            titleFindMs = System.currentTimeMillis() - stepStart;

            return new ProductPriceInfo(productName, null, null, null, "PttAvm", productUrl, driverInitMs, pageLoadMs, titleFindMs, priceFindMs);
        } catch (Exception e) {
            return new ProductPriceInfo("Hata: " + e.getMessage(), null, null, null, "PttAvm", productUrl, driverInitMs, pageLoadMs, titleFindMs, priceFindMs);
        } finally {
            if (driver != null) driver.quit();
        }
    }
}