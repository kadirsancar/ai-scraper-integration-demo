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
import java.math.RoundingMode;
import java.time.Duration;

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

        String searchUrl =
                "https://www.pttavm.com/arama?q="
                        + productName.replace(" ", "+");

        WebDriver driver = null;

        try {

            ChromeOptions options = createChromeOptions();

            driver = new ChromeDriver(options);

            driver.get(searchUrl);

            WebDriverWait wait =
                    new WebDriverWait(driver, Duration.ofSeconds(10));

            WebElement firstProduct = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    "a.product-item, " +
                                            "div.product-card a, " +
                                            "a[href*='/urun/'], " +
                                            "a[href*='/product/'], " +
                                            "a[href*='-p-']"
                            )
                    )
            );

            String productUrl =
                    firstProduct.getAttribute("href");

            if (productUrl == null || productUrl.isBlank()) {

                return new ProductPriceInfo(
                        "PttAVM ürün URL'si bulunamadı",
                        null,
                        null,
                        null,
                        "PttAvm",
                        searchUrl,
                        0,
                        0,
                        0,
                        0
                );
            }

            return getProductDetails(productUrl);

        } catch (Exception e) {

            return new ProductPriceInfo(
                    "Arama Hatası: " + e.getMessage(),
                    null,
                    null,
                    null,
                    "PttAvm",
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

    private ProductPriceInfo fetchProductDetailsInternal(
            String productUrl) {

        WebDriver driver = null;

        String productName =
                "Bilinmeyen PttAvm Ürünü";

        BigDecimal price = null;

        // Sitedeki üstü çizili eski fiyat
        BigDecimal oldPrice = null;

        // Örneğin: %5.90 indirim (619.44 TL Kazanç)
        String discountInfo = null;

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            // =====================================================
            // DRIVER
            // =====================================================

            long stepStart =
                    System.currentTimeMillis();

            ChromeOptions options =
                    createChromeOptions();

            driver =
                    new ChromeDriver(options);

            driverInitMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // PAGE LOAD
            // =====================================================

            stepStart =
                    System.currentTimeMillis();

            try {
                driver.get(productUrl);
            } catch (Exception ignored) {
            }

            pageLoadMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // WAIT
            // =====================================================

            WebDriverWait wait =
                    new WebDriverWait(
                            driver,
                            Duration.ofSeconds(10)
                    );

            try {

                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector(
                                        "h1, " +
                                                ".specialPriceValue__HPhRC, " +
                                                ".regularPriceValue__I3siB"
                                )
                        )
                );

            } catch (Exception ignored) {
            }


            // =====================================================
            // BOT CHECK
            // =====================================================

            String pageSource =
                    driver.getPageSource();

            if (isBlocked(pageSource)) {

                System.out.println(
                        "PttAVM bot engellemesi tespit edildi."
                );

                return new ProductPriceInfo(
                        "PttAVM tarafından erişim engellendi",
                        null,
                        null,
                        null,
                        "PttAvm",
                        productUrl,
                        driverInitMs,
                        pageLoadMs,
                        0,
                        0
                );
            }


            // =====================================================
            // PRODUCT TITLE
            // =====================================================

            stepStart =
                    System.currentTimeMillis();

            String[] titleSelectors = {

                    "h1",

                    "[class*='productTitle']",

                    "[class*='product-title']",

                    "[class*='productName']",

                    "[class*='product-name']"
            };

            for (String selector : titleSelectors) {

                try {

                    WebElement element =
                            driver.findElement(
                                    By.cssSelector(selector)
                            );

                    String text =
                            element.getText().trim();

                    if (!text.isEmpty()) {

                        productName = text;

                        System.out.println(
                                "PttAVM ürün adı: "
                                        + productName
                        );

                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            titleFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // PRICE
            // =====================================================

            stepStart =
                    System.currentTimeMillis();


            // =====================================================
            // 1. İNDİRİMLİ / GÜNCEL FİYAT
            // =====================================================

            try {

                WebElement specialPriceElement =
                        driver.findElement(
                                By.cssSelector(
                                        ".specialPriceValue__HPhRC"
                                )
                        );

                String specialPriceText =
                        specialPriceElement
                                .getText()
                                .trim();

                System.out.println(
                        "PttAVM indirimli fiyat: "
                                + specialPriceText
                );

                price =
                        parsePrice(specialPriceText);

            } catch (Exception e) {

                System.out.println(
                        "PttAVM indirimli fiyat bulunamadı."
                );
            }


            // =====================================================
            // 2. ESKİ / İNDİRİMSİZ FİYAT
            // =====================================================

            try {

                WebElement regularPriceElement =
                        driver.findElement(
                                By.cssSelector(
                                        ".regularPriceValue__I3siB"
                                )
                        );

                String regularPriceText =
                        regularPriceElement
                                .getText()
                                .trim();

                System.out.println(
                        "PttAVM eski fiyat: "
                                + regularPriceText
                );

                oldPrice =
                        parsePrice(regularPriceText);

            } catch (Exception e) {

                System.out.println(
                        "PttAVM eski fiyat bulunamadı."
                );
            }


            // =====================================================
            // 3. DISCOUNT INFO HESAPLA
            // =====================================================

            discountInfo =
                    calculateDiscountInfo(
                            oldPrice,
                            price
                    );


            priceFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // RESULT LOG
            // =====================================================

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "PttAVM ÜRÜN"
            );

            System.out.println(
                    "Ürün: " + productName
            );

            System.out.println(
                    "Yeni fiyat: " + price
            );

            System.out.println(
                    "Eski fiyat: " + oldPrice
            );

            System.out.println(
                    "Discount info: " + discountInfo
            );

            System.out.println(
                    "========================================"
            );


            // =====================================================
            // RETURN
            // =====================================================

            return new ProductPriceInfo(
                    productName,
                    price,
                    oldPrice,
                    discountInfo,
                    "PttAvm",
                    productUrl,
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );


        } catch (Exception e) {

            System.out.println(
                    "PttAVM scraper hatası: "
                            + e.getMessage()
            );

            return new ProductPriceInfo(
                    "Hata: " + e.getMessage(),
                    null,
                    null,
                    null,
                    "PttAvm",
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


    // =========================================================
    // DISCOUNT CALCULATION
    // =========================================================

    private String calculateDiscountInfo(
            BigDecimal oldPrice,
            BigDecimal newPrice) {

        // İki fiyat da yoksa
        if (oldPrice == null || newPrice == null) {
            return null;
        }

        // Eski fiyat 0 veya negatifse hesaplama yapma
        if (oldPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // Yeni fiyat eski fiyattan büyük/eşitse indirim yok
        if (newPrice.compareTo(oldPrice) >= 0) {
            return null;
        }

        // Kazanç
        BigDecimal saving =
                oldPrice.subtract(newPrice);

        // İndirim yüzdesi
        BigDecimal discountPercentage =
                saving
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                oldPrice,
                                2,
                                RoundingMode.HALF_UP
                        );

        saving =
                saving.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        return "%" +
                discountPercentage +
                " indirim (" +
                saving +
                " TL Kazanç)";
    }


    // =========================================================
    // CHROME OPTIONS
    // =========================================================

    private ChromeOptions createChromeOptions() {

        ChromeOptions options =
                new ChromeOptions();

        options.addArguments(
                "--headless=new"
        );

        options.addArguments(
                "--disable-blink-features=AutomationControlled"
        );

        options.addArguments(
                "--disable-gpu"
        );

        options.addArguments(
                "--no-sandbox"
        );

        options.addArguments(
                "--disable-dev-shm-usage"
        );

        options.addArguments(
                "--window-size=1920,1080"
        );

        options.addArguments(
                "--lang=tr-TR"
        );

        options.addArguments(
                "--user-agent=Mozilla/5.0 " +
                        "(Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) " +
                        "Chrome/151.0.0.0 " +
                        "Safari/537.36"
        );

        options.setPageLoadStrategy(
                PageLoadStrategy.EAGER
        );

        return options;
    }


    // =========================================================
    // BLOCK CHECK
    // =========================================================

    private boolean isBlocked(String pageSource) {

        if (pageSource == null) {
            return false;
        }

        String source =
                pageSource.toLowerCase();

        return source.contains(
                "sorry, you have been blocked"
        )
                || source.contains(
                "you have been blocked"
        )
                || source.contains(
                "access denied"
        )
                || source.contains(
                "cf-chl-error"
        )
                || source.contains(
                "cf-chl-captcha"
        )
                || source.contains(
                "checking your browser"
        );
    }


    // =========================================================
    // PRICE PARSER
    // =========================================================

    private BigDecimal parsePrice(String priceText) {

        try {

            if (priceText == null ||
                    priceText.isBlank()) {

                return null;
            }

            String cleaned =
                    priceText
                            .replace("TL", "")
                            .replace("tl", "")
                            .replace("₺", "")
                            .trim();


            /*
             * 9.879,56
             *
             * ->
             *
             * 9879.56
             */

            if (cleaned.contains(",")) {

                cleaned =
                        cleaned
                                .replace(".", "")
                                .replace(",", ".");

            } else {

                /*
                 * 10.499 TL
                 *
                 * ->
                 *
                 * 10499
                 */

                cleaned =
                        cleaned.replace(".", "");
            }


            cleaned =
                    cleaned.replaceAll(
                            "[^0-9.]",
                            ""
                    );


            if (cleaned.isEmpty()) {
                return null;
            }

            return new BigDecimal(cleaned);

        } catch (Exception e) {

            System.out.println(
                    "PttAVM fiyat parse edilemedi: "
                            + priceText
            );

            return null;
        }
    }
}