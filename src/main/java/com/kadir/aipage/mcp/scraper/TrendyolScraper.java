package com.kadir.aipage.mcp.scraper;

import com.kadir.aipage.dto.ProductPriceInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class TrendyolScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null && url.toLowerCase().contains("trendyol.com");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        return fetchProductDetailsInternal(productUrl);
    }

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {

        String searchUrl = "https://www.trendyol.com/sr?q="
                + productName.replace(" ", "+");

        WebDriver driver = null;

        try {

            ChromeOptions options = new ChromeOptions();

            // Şimdilik test için headless kapalı.
            // Her şey çalıştıktan sonra tekrar açabiliriz.
            // options.addArguments("--headless=new");

            options.addArguments("--window-size=1920,1080");
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);

            driver.get(searchUrl);

            WebDriverWait wait =
                    new WebDriverWait(driver, Duration.ofSeconds(10));

            WebElement firstProduct = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.p-card-wrppr a")
                    )
            );

            String href = firstProduct.getAttribute("href");

            String productUrl;

            if (href.startsWith("http")) {
                productUrl = href;
            } else {
                productUrl = "https://www.trendyol.com" + href;
            }

            System.out.println("Arama sonucu ürün URL: " + productUrl);

            return getProductDetails(productUrl);

        } catch (Exception e) {

            return new ProductPriceInfo(
                    "Arama Hatası: " + e.getMessage(),
                    null,
                    null,
                    null,
                    "Trendyol",
                    searchUrl,
                    0,
                    0,
                    0,
                    0
            );

        } finally {

            if (driver != null) {
                driver.quit();
            }
        }
    }

    private ProductPriceInfo fetchProductDetailsInternal(String productUrl) {

        WebDriver driver = null;

        String productName = "Bilinmeyen Trendyol Ürünü";

        BigDecimal currentPrice = null;

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            // ==================================================
            // 1. CHROME BAŞLAT
            // ==================================================

            long stepStart = System.currentTimeMillis();

            ChromeOptions options = new ChromeOptions();

            // Şimdilik headless kapalı.
            // Testler bittikten sonra açabiliriz.
            // options.addArguments("--headless=new");

            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");

            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);

            driverInitMs =
                    System.currentTimeMillis() - stepStart;


            // ==================================================
            // 2. ÜRÜN SAYFASINI AÇ
            // ==================================================

            stepStart = System.currentTimeMillis();

            try {
                driver.get(productUrl);
            } catch (Exception ignored) {
            }

            System.out.println(
                    "CURRENT URL: " + driver.getCurrentUrl()
            );

            System.out.println(
                    "TITLE: " + driver.getTitle()
            );

            System.out.println(
                    "PAGE SOURCE LENGTH: "
                            + driver.getPageSource().length()
            );

            pageLoadMs =
                    System.currentTimeMillis() - stepStart;


            // ==================================================
            // 3. SAYFANIN OTURMASINI BEKLE
            // ==================================================

            Thread.sleep(3000);


            // ==================================================
            // 4. ÜRÜN ADINI BUL
            // ==================================================

            stepStart = System.currentTimeMillis();

            String[] titleSelectors = {

                    "h1[data-testid='product-title']",

                    ".pr-new-br",

                    "[data-testid='product-name']",

                    "h1"
            };

            for (String selector : titleSelectors) {

                try {

                    WebElement titleElement =
                            driver.findElement(
                                    By.cssSelector(selector)
                            );

                    String text = titleElement.getText();

                    if (text != null && !text.isBlank()) {

                        productName = text.trim();

                        System.out.println(
                                "Ürün adı bulundu. Selector: "
                                        + selector
                        );

                        System.out.println(
                                "Ürün adı: " + productName
                        );

                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            titleFindMs =
                    System.currentTimeMillis() - stepStart;


            // ==================================================
            // 5. FİYATI BUL
            // ==================================================

            stepStart = System.currentTimeMillis();

            WebDriverWait wait =
                    new WebDriverWait(
                            driver,
                            Duration.ofSeconds(10)
                    );

            WebElement priceElement = null;

            /*
             * Trendyol'un incelediğimiz gerçek HTML yapısı:
             *
             * div.price-wrapper
             *     div.price.normal-price
             *         div.price-container
             *             span.discounted
             *
             * Örnek:
             *
             * <span class="discounted">
             *     1.643 TL
             * </span>
             */

            try {

                priceElement = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector(
                                        "div.price-container span.discounted"
                                )
                        )
                );

                String priceText =
                        priceElement.getText();

                System.out.println(
                        "Fiyat elementi bulundu."
                );

                System.out.println(
                        "Ham fiyat: " + priceText
                );

                currentPrice =
                        parsePrice(priceText);

            } catch (Exception e) {

                System.out.println(
                        "Ana fiyat selector'ı ile fiyat bulunamadı."
                );

                System.out.println(
                        "Hata: " + e.getMessage()
                );
            }


            // ==================================================
            // 6. İKİNCİ FİYAT SELECTOR'I
            // ==================================================

            if (currentPrice == null) {

                try {

                    priceElement = driver.findElement(
                            By.cssSelector("span.discounted")
                    );

                    String priceText =
                            priceElement.getText();

                    System.out.println(
                            "Alternatif fiyat selector'ı bulundu."
                    );

                    System.out.println(
                            "Ham fiyat: " + priceText
                    );

                    currentPrice =
                            parsePrice(priceText);

                } catch (Exception e) {

                    System.out.println(
                            "Alternatif fiyat selector'ı da başarısız."
                    );
                }
            }


            priceFindMs =
                    System.currentTimeMillis() - stepStart;


            // ==================================================
            // 7. SONUÇLARI YAZDIR
            // ==================================================

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Trendyol ürün adı: " + productName
            );

            System.out.println(
                    "Trendyol bulunan fiyat: " + currentPrice
            );

            System.out.println(
                    "======================================"
            );


            // ==================================================
            // 8. PRODUCT PRICE INFO
            // ==================================================

            return new ProductPriceInfo(
                    productName,
                    currentPrice,
                    null,
                    null,
                    "Trendyol",
                    productUrl,
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );


        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return new ProductPriceInfo(
                    "Trendyol işlemi kesintiye uğradı",
                    null,
                    null,
                    null,
                    "Trendyol",
                    productUrl,
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );

        } catch (Exception e) {

            return new ProductPriceInfo(
                    "Hata: " + e.getMessage(),
                    null,
                    null,
                    null,
                    "Trendyol",
                    productUrl,
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );

        } finally {

            if (driver != null) {
                driver.quit();
            }
        }
    }


    // ==================================================
    // FİYATI BIGDECIMAL'A ÇEVİR
    // ==================================================

    private BigDecimal parsePrice(String priceText) {

        if (priceText == null || priceText.isBlank()) {
            return null;
        }

        try {


            String clean =
                    priceText.replaceAll("[^0-9.,]", "");

            if (clean.isBlank()) {
                return null;
            }

            if (clean.contains(".") && clean.contains(",")) {

                clean = clean
                        .replace(".", "")
                        .replace(",", ".");

            } else if (clean.contains(",")) {

                clean = clean.replace(",", ".");
            }

            return new BigDecimal(clean);

        } catch (Exception e) {

            System.out.println(
                    "Fiyat parse edilemedi: "
                            + priceText
            );

            return null;
        }
    }
}