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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

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

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            System.out.println("======================================");
            System.out.println("PTTAVM ürün araması: " + productName);
            System.out.println("PTTAVM arama URL: " + searchUrl);
            System.out.println("======================================");

            // =====================================================
            // DRIVER
            // =====================================================

            long stepStart = System.currentTimeMillis();

            ChromeOptions options = createChromeOptions();

            driver = new ChromeDriver(options);

            driverInitMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // PAGE LOAD
            // =====================================================

            stepStart = System.currentTimeMillis();

            try {
                driver.get(searchUrl);
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

            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(
                                    "ul[data-grid='product-list'] li"
                            )
                    )
            );


            // =====================================================
            // BOT CHECK
            // =====================================================

            if (isBlocked(driver.getPageSource())) {

                System.out.println(
                        "❌ PTTAVM bot engellemesi tespit edildi."
                );

                return new ProductPriceInfo(
                        "PttAVM tarafından erişim engellendi",
                        null,
                        null,
                        null,
                        "PttAvm",
                        searchUrl,
                        driverInitMs,
                        pageLoadMs,
                        0,
                        0
                );
            }


            // =====================================================
            // İLK ÜRÜN = 0. ELEMENT
            // =====================================================

            WebElement firstProduct =
                    driver.findElement(
                            By.cssSelector(
                                    "ul[data-grid='product-list'] li:first-child"
                            )
                    );


            // =====================================================
            // PRODUCT URL
            // =====================================================

            WebElement productLink =
                    firstProduct.findElement(
                            By.cssSelector(
                                    "a.card__dfYph"
                            )
                    );

            String productUrl =
                    productLink.getAttribute("href");


            if (productUrl == null ||
                    productUrl.isBlank()) {

                return new ProductPriceInfo(
                        "PttAVM ürün URL'si bulunamadı",
                        null,
                        null,
                        null,
                        "PttAvm",
                        searchUrl,
                        driverInitMs,
                        pageLoadMs,
                        0,
                        0
                );
            }


            // =====================================================
            // PRODUCT TITLE
            // =====================================================

            stepStart = System.currentTimeMillis();

            String foundProductName;

            try {

                WebElement nameElement =
                        firstProduct.findElement(
                                By.cssSelector(
                                        "h2.name__yWPWa"
                                )
                        );

                foundProductName =
                        nameElement.getText().trim();

            } catch (Exception e) {

                foundProductName =
                        productName;
            }

            titleFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // PRICE
            // =====================================================

            stepStart = System.currentTimeMillis();

            BigDecimal price = null;
            BigDecimal oldPrice = null;


            // -----------------------------------------------------
            // GÜNCEL / SEPETE ÖZEL FİYAT
            // -----------------------------------------------------

            try {

                WebElement specialPriceElement =
                        firstProduct.findElement(
                                By.cssSelector(
                                        ".specialPriceValue__HPhRC"
                                )
                        );

                String specialPriceText =
                        specialPriceElement
                                .getText()
                                .trim();

                price =
                        parsePrice(specialPriceText);

            } catch (Exception e) {

                System.out.println(
                        "PTTAVM sepete özel fiyat bulunamadı."
                );
            }


            // -----------------------------------------------------
            // NORMAL / ESKİ FİYAT
            // -----------------------------------------------------

            try {

                WebElement regularPriceElement =
                        firstProduct.findElement(
                                By.cssSelector(
                                        ".regularPriceValue__I3siB"
                                )
                        );

                String regularPriceText =
                        regularPriceElement
                                .getText()
                                .trim();

                oldPrice =
                        parsePrice(regularPriceText);

            } catch (Exception e) {

                System.out.println(
                        "PTTAVM normal fiyat bulunamadı."
                );
            }


            // =====================================================
            // DISCOUNT
            // =====================================================

            String discountInfo =
                    calculateDiscountInfo(
                            oldPrice,
                            price
                    );


            priceFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // LOG
            // =====================================================

            System.out.println(
                    "PTTAVM ilk ürün bulundu:"
            );

            System.out.println(
                    "Ürün: " + foundProductName
            );

            System.out.println(
                    "URL: " + productUrl
            );

            System.out.println(
                    "Güncel fiyat: " + price
            );

            System.out.println(
                    "Normal fiyat: " + oldPrice
            );

            System.out.println(
                    "İndirim: " + discountInfo
            );


            // =====================================================
            // RETURN
            // =====================================================

            return new ProductPriceInfo(
                    foundProductName,
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
                    "❌ PTTAVM arama hatası: "
                            + e.getMessage()
            );

            return new ProductPriceInfo(
                    "Arama Hatası: " + e.getMessage(),
                    null,
                    null,
                    null,
                    "PttAvm",
                    searchUrl,
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
    // PRODUCT DETAIL
    // =========================================================

    private ProductPriceInfo fetchProductDetailsInternal(
            String productUrl) {

        WebDriver driver = null;

        String productName =
                "Bilinmeyen PttAvm Ürünü";

        BigDecimal price = null;

        BigDecimal oldPrice = null;

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
                            Duration.ofSeconds(15)
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
                        "❌ PTTAVM bot engellemesi tespit edildi."
                );

                return createErrorResult(
                        "PTTAVM tarafından erişim engellendi",
                        productUrl,
                        driverInitMs,
                        pageLoadMs
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
                                "PTTAVM ürün adı: "
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
            // CURRENT / SPECIAL PRICE
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
                        "PTTAVM güncel fiyat: "
                                + specialPriceText
                );

                price =
                        parsePrice(
                                specialPriceText
                        );

            } catch (Exception e) {

                System.out.println(
                        "PTTAVM sepete özel fiyat bulunamadı."
                );
            }


            // =====================================================
            // REGULAR PRICE
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
                        "PTTAVM normal fiyat: "
                                + regularPriceText
                );

                oldPrice =
                        parsePrice(
                                regularPriceText
                        );

            } catch (Exception e) {

                System.out.println(
                        "PTTAVM normal fiyat bulunamadı."
                );
            }


            // =====================================================
            // FALLBACK
            // =====================================================

            if (price == null) {

                try {

                    String[] fallbackSelectors = {

                            "[class*='specialPriceValue']",

                            "[class*='salePrice']",

                            "[class*='sale-price']",

                            "[class*='currentPrice']",

                            "[class*='current-price']",

                            "[class*='priceValue']"
                    };

                    for (String selector :
                            fallbackSelectors) {

                        try {

                            WebElement element =
                                    driver.findElement(
                                            By.cssSelector(
                                                    selector
                                            )
                                    );

                            String text =
                                    element
                                            .getText()
                                            .trim();

                            BigDecimal parsed =
                                    parsePrice(text);

                            if (parsed != null) {

                                price = parsed;

                                System.out.println(
                                        "PTTAVM fallback fiyat: "
                                                + text
                                );

                                break;
                            }

                        } catch (Exception ignored) {
                        }
                    }

                } catch (Exception ignored) {
                }
            }


            // =====================================================
            // DISCOUNT
            // =====================================================

            discountInfo =
                    calculateDiscountInfo(
                            oldPrice,
                            price
                    );


            priceFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // RESULT
            // =====================================================

            System.out.println(
                    "========================================"
            );

            System.out.println(
                    "PTTAVM ÜRÜN"
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
                    "❌ PTTAVM scraper hatası: "
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

        if (oldPrice == null || newPrice == null) {
            return null;
        }

        if (oldPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        if (newPrice.compareTo(oldPrice) >= 0) {
            return null;
        }

        BigDecimal saving =
                oldPrice.subtract(newPrice);

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
    // PRODUCT NAME NORMALIZATION
    // =========================================================

    private String normalizeProductName(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .replaceAll(
                        "[^a-z0-9]+",
                        " "
                )
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }


    // =========================================================
    // ERROR RESULT
    // =========================================================

    private ProductPriceInfo createErrorResult(
            String message,
            String url,
            long driverInitMs,
            long pageLoadMs) {

        return new ProductPriceInfo(
                message,
                null,
                null,
                null,
                "PttAvm",
                url,
                driverInitMs,
                pageLoadMs,
                0,
                0
        );
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

    private boolean isBlocked(
            String pageSource) {

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

    private BigDecimal parsePrice(
            String priceText) {

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


            if (cleaned.contains(",")) {

                cleaned =
                        cleaned
                                .replace(".", "")
                                .replace(",", ".");

            } else {

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
                    "PTTAVM fiyat parse edilemedi: "
                            + priceText
            );

            return null;
        }
    }
}