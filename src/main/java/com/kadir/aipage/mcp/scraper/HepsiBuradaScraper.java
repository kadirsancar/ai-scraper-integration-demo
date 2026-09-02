
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
import java.util.List;

@Component
public class HepsiBuradaScraper implements  ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null &&
                url.toLowerCase().contains("hepsiburada.com");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        return fetchProductDetailsInternal(productUrl);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {

        String searchUrl =
                "https://www.hepsiburada.com/ara?q="
                        + productName.replace(" ", "+");

        WebDriver driver = null;

        try {

            System.out.println("======================================");
            System.out.println("Hepsiburada ürün araması: " + productName);
            System.out.println("Hepsiburada arama URL: " + searchUrl);
            System.out.println("======================================");

            ChromeOptions options = createChromeOptions();

            driver = new ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(Duration.ofSeconds(30));

            driver.get(searchUrl);

            Thread.sleep(5000);

            System.out.println(
                    "Hepsiburada sayfa title: "
                            + driver.getTitle()
            );

            System.out.println(
                    "Hepsiburada mevcut URL: "
                            + driver.getCurrentUrl()
            );

            // =====================================================
            // SEARCH SECURITY CHECK
            // =====================================================

            if (isSecurityPage(driver)) {

                System.out.println(
                        "❌ Hepsiburada arama sayfasında güvenlik kontrolü!"
                );

                return new ProductPriceInfo(
                        "Hepsiburada | Güvenlik",
                        null,
                        null,
                        null,
                        "Hepsiburada",
                        searchUrl,
                        0,
                        0,
                        0,
                        0
                );
            }

            // =====================================================
            // ÜRÜN LİNKLERİNİ BUL
            // =====================================================

            String[] selectors = {

                    "a[href*='-p-']",

                    "a[class*='productCardLink']",

                    "a[href*='/']"

            };

            WebElement firstProduct = null;

            for (String selector : selectors) {

                try {

                    List<WebElement> products =
                            driver.findElements(
                                    By.cssSelector(selector)
                            );

                    System.out.println(
                            "Selector: "
                                    + selector
                                    + " | element sayısı: "
                                    + products.size()
                    );

                    for (WebElement product : products) {

                        String href =
                                product.getAttribute("href");

                        if (href == null ||
                                href.isBlank()) {
                            continue;
                        }

                        if (href.contains("-p-")) {

                            firstProduct = product;

                            System.out.println(
                                    "✅ Ürün linki bulundu:"
                            );

                            System.out.println(
                                    "URL: " + href
                            );

                            break;
                        }
                    }

                    if (firstProduct != null) {
                        break;
                    }

                } catch (Exception e) {

                    System.out.println(
                            "Selector hatası: "
                                    + selector
                                    + " -> "
                                    + e.getMessage()
                    );
                }
            }

            // =====================================================
            // ÜRÜN BULUNAMADI
            // =====================================================

            if (firstProduct == null) {

                System.out.println(
                        "❌ Hepsiburada ürün linki bulunamadı."
                );

                return new ProductPriceInfo(
                        "Arama sonucu bulunamadı",
                        null,
                        null,
                        null,
                        "Hepsiburada",
                        searchUrl,
                        0,
                        0,
                        0,
                        0
                );
            }

            // =====================================================
            // ÜRÜN URL
            // =====================================================

            String productUrl =
                    firstProduct.getAttribute("href");

            if (productUrl != null &&
                    productUrl.startsWith("/")) {

                productUrl =
                        "https://www.hepsiburada.com"
                                + productUrl;
            }

            /*
             * Satıcı parametresini kaldır.
             *
             * Örnek:
             *
             * ?magaza=Nethouse
             *
             */

            if (productUrl != null &&
                    productUrl.contains("?")) {

                productUrl =
                        productUrl.substring(
                                0,
                                productUrl.indexOf("?")
                        );
            }

            // =====================================================
            // ÜRÜN ADI
            // =====================================================

            String searchProductName =
                    firstProduct.getAttribute("title");

            if (searchProductName == null ||
                    searchProductName.isBlank()) {

                searchProductName =
                        firstProduct.getText().trim();
            }

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Hepsiburada ilk ürün bulundu:"
            );

            System.out.println(
                    "Ürün: " + searchProductName
            );

            System.out.println(
                    "Temiz URL: " + productUrl
            );

            System.out.println(
                    "======================================"
            );

            // =====================================================
            // ÜRÜN DETAYI
            // =====================================================

            return getProductDetails(productUrl);

        } catch (Exception e) {

            System.out.println(
                    "❌ Hepsiburada arama hatası: "
                            + e.getMessage()
            );

            return new ProductPriceInfo(
                    "Arama Hatası: " + e.getMessage(),
                    null,
                    null,
                    null,
                    "Hepsiburada",
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

    // =========================================================
    // PRODUCT DETAIL
    // =========================================================

    private ProductPriceInfo fetchProductDetailsInternal(
            String productUrl) {

        WebDriver driver = null;

        String productName =
                "Bilinmeyen Hepsiburada Ürünü";

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            // =====================================================
            // DRIVER
            // =====================================================

            long start =
                    System.currentTimeMillis();

            ChromeOptions options =
                    createChromeOptions();

            driver =
                    new org.openqa.selenium.chrome.ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(
                            Duration.ofSeconds(30)
                    );

            driverInitMs =
                    System.currentTimeMillis() - start;

            // =====================================================
            // PAGE LOAD
            // =====================================================

            start =
                    System.currentTimeMillis();

            try {

                System.out.println(
                        "Hepsiburada detay sayfası açılıyor..."
                );

                System.out.println(
                        "URL: " + productUrl
                );

                driver.get(productUrl);

            } catch (Exception e) {

                System.out.println(
                        "Detay sayfası yükleme uyarısı: "
                                + e.getMessage()
                );
            }

            /*
             * JavaScript'in çalışması ve fiyat alanlarının
             * oluşması için kısa bekleme.
             */

            Thread.sleep(5000);

            pageLoadMs =
                    System.currentTimeMillis() - start;

            // =====================================================
            // PAGE INFO
            // =====================================================

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Hepsiburada detay URL: "
                            + driver.getCurrentUrl()
            );

            System.out.println(
                    "Hepsiburada Title: "
                            + driver.getTitle()
            );

            System.out.println(
                    "======================================"
            );

            // =====================================================
            // SECURITY CHECK
            // =====================================================

            if (isSecurityPage(driver)) {

                System.out.println(
                        "❌ HEPSIBURADA GÜVENLİK SAYFASI!"
                );

                System.out.println(
                        "Security page URL: "
                                + driver.getCurrentUrl()
                );

                System.out.println(
                        "Security page title: "
                                + driver.getTitle()
                );

                return new ProductPriceInfo(
                        "Hepsiburada | Güvenlik",
                        null,
                        null,
                        null,
                        "Hepsiburada",
                        productUrl,
                        driverInitMs,
                        pageLoadMs,
                        titleFindMs,
                        priceFindMs
                );
            }

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
                                        "h1, [data-test-id='title'], [data-test-id^='final-price'], [data-test-id='default-price']"
                                )
                        )
                );

            } catch (Exception e) {

                System.out.println(
                        "Fiyat/title elementi bekleme süresi doldu."
                );
            }

            // =====================================================
            // PRODUCT NAME
            // =====================================================

            start =
                    System.currentTimeMillis();

            productName =
                    findProductName(driver);

            titleFindMs =
                    System.currentTimeMillis() - start;

            // =====================================================
            // PRICES
            // =====================================================

            start =
                    System.currentTimeMillis();

            BigDecimal currentPrice =
                    findCurrentPrice(driver);

            BigDecimal previousPrice =
                    findPreviousPrice(driver);

            // =====================================================
            // DISCOUNT
            // =====================================================

            String discountPercentage =
                    calculateDiscount(
                            currentPrice,
                            previousPrice
                    );

            priceFindMs =
                    System.currentTimeMillis() - start;

            // =====================================================
            // DEBUG
            // =====================================================

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Hepsiburada Ürün: "
                            + productName
            );

            System.out.println(
                    "Güncel Fiyat: "
                            + currentPrice
            );

            System.out.println(
                    "Önceki Fiyat: "
                            + previousPrice
            );

            System.out.println(
                    "İndirim: %"
                            + discountPercentage
            );

            System.out.println(
                    "======================================"
            );

            return new ProductPriceInfo(
                    productName,
                    currentPrice,
                    previousPrice,
                    discountPercentage,
                    "Hepsiburada",
                    productUrl,
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );

        } catch (Exception e) {

            System.out.println(
                    "❌ Hepsiburada scraper hatası: "
                            + e.getMessage()
            );

            e.printStackTrace();

            return new ProductPriceInfo(
                    productName,
                    null,
                    null,
                    null,
                    "Hepsiburada",
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
    // CHROME OPTIONS
    // =========================================================

    private ChromeOptions createChromeOptions() {

        ChromeOptions options =
                new ChromeOptions();

        /*
         * HEADLESS KAPALI.
         */

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
                "--disable-extensions"
        );

        options.addArguments(
                "--disable-notifications"
        );

        options.setPageLoadStrategy(
                PageLoadStrategy.EAGER
        );

        return options;
    }

    // =========================================================
    // SECURITY PAGE
    // =========================================================

    private boolean isSecurityPage(
            WebDriver driver) {

        try {

            String title =
                    driver.getTitle();

            String currentUrl =
                    driver.getCurrentUrl();

            System.out.println(
                    "Security check title: "
                            + title
            );

            System.out.println(
                    "Security check URL: "
                            + currentUrl
            );

            // =====================================================
            // URL KONTROLÜ
            // =====================================================

            String lowerUrl =
                    currentUrl != null
                            ? currentUrl.toLowerCase()
                            : "";

            if (lowerUrl.contains("captcha") ||
                    lowerUrl.contains("challenge") ||
                    lowerUrl.contains("security-check") ||
                    lowerUrl.contains("security_check")) {

                return true;
            }

            // =====================================================
            // TITLE KONTROLÜ
            // =====================================================

            String lowerTitle =
                    title != null
                            ? title.toLowerCase()
                            : "";

            if (lowerTitle.contains("security check") ||
                    lowerTitle.contains("güvenlik kontrolü") ||
                    lowerTitle.contains("captcha") ||
                    lowerTitle.contains("doğrulama gerekli")) {

                return true;
            }

            // =====================================================
            // BODY KONTROLÜ
            // =====================================================

            String body =
                    driver.findElement(
                            By.tagName("body")
                    ).getText();

            if (body == null ||
                    body.isBlank()) {

                return false;
            }

            String lowerBody =
                    body.toLowerCase();

            /*
             * ÖNEMLİ:
             *
             * Artık sadece "güvenlik" ve "hepsiburada"
             * kelimelerinin aynı anda bulunmasına
             * bakmıyoruz.
             *
             * Çünkü normal ürün sayfasında da bu
             * kelimeler bulunabilir.
             */

            if (lowerBody.contains(
                    "güvenlik kontrolünü tamamlayın")) {

                return true;
            }

            if (lowerBody.contains(
                    "güvenlik doğrulaması")) {

                return true;
            }

            if (lowerBody.contains(
                    "robot olmadığınızı doğrulayın")) {

                return true;
            }

            if (lowerBody.contains(
                    "insan olduğunuzu doğrulayın")) {

                return true;
            }

            if (lowerBody.contains(
                    "captcha")) {

                return true;
            }

            return false;

        } catch (Exception e) {

            System.out.println(
                    "Security check kontrolünde hata: "
                            + e.getMessage()
            );

            return false;
        }
    }

    // =========================================================
    // PRODUCT NAME
    // =========================================================

    private String findProductName(
            WebDriver driver) {

        // 1 - product-name

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.id("product-name")
                    );

            for (WebElement element : elements) {

                String text =
                        element.getText().trim();

                if (!text.isBlank()) {
                    return text;
                }
            }

        } catch (Exception ignored) {
        }

        // 2 - data-test-id title

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    "[data-test-id='title']"
                            )
                    );

            for (WebElement element : elements) {

                String text =
                        element.getText().trim();

                if (!text.isBlank()) {
                    return text;
                }
            }

        } catch (Exception ignored) {
        }

        // 3 - H1

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector("h1")
                    );

            for (WebElement element : elements) {

                String text =
                        element.getText().trim();

                if (!text.isBlank()) {
                    return text;
                }
            }

        } catch (Exception ignored) {
        }

        // 4 - Browser title

        try {

            String title =
                    driver.getTitle();

            if (title != null &&
                    !title.isBlank()) {

                return title
                        .replace(
                                " | Hepsiburada",
                                ""
                        )
                        .replace(
                                " Fiyatı",
                                ""
                        )
                        .trim();
            }

        } catch (Exception ignored) {
        }

        return "Bilinmeyen Hepsiburada Ürünü";
    }

    // =========================================================
    // CURRENT PRICE
    // =========================================================

    private BigDecimal findCurrentPrice(
            WebDriver driver) {

        String[] selectors = {

                "[data-test-id^='final-price']",

                "[data-test-id='default-price']",

                "[class*='finalPrice']",

                "[class*='final-price']",

                "[class*='currentPrice']",

                "[class*='current-price']"

        };

        for (String selector : selectors) {

            try {

                List<WebElement> elements =
                        driver.findElements(
                                By.cssSelector(selector)
                        );

                System.out.println(
                        "Güncel fiyat selector: "
                                + selector
                                + " | element: "
                                + elements.size()
                );

                for (WebElement element : elements) {

                    String text =
                            element.getText().trim();

                    System.out.println(
                            "Güncel fiyat adayı: "
                                    + text
                    );

                    BigDecimal price =
                            parsePrice(text);

                    if (isValidPrice(price)) {

                        System.out.println(
                                "✅ Güncel fiyat bulundu: "
                                        + price
                        );

                        return price;
                    }
                }

            } catch (Exception e) {

                System.out.println(
                        "Güncel fiyat selector hatası: "
                                + selector
                                + " -> "
                                + e.getMessage()
                );
            }
        }

        return null;
    }

    // =========================================================
    // PREVIOUS PRICE
    // =========================================================

    private BigDecimal findPreviousPrice(
            WebDriver driver) {

        String[] selectors = {

                "[data-test-id='prev-price'] span",

                "[data-test-id='prev-price']",

                "[class*='originalPrice']",

                "[class*='original-price']",

                "[class*='previousPrice']",

                "[class*='previous-price']",

                "del"

        };

        for (String selector : selectors) {

            try {

                List<WebElement> elements =
                        driver.findElements(
                                By.cssSelector(selector)
                        );

                System.out.println(
                        "Önceki fiyat selector: "
                                + selector
                                + " | element: "
                                + elements.size()
                );

                for (WebElement element : elements) {

                    String text =
                            element.getText().trim();

                    System.out.println(
                            "Önceki fiyat adayı: "
                                    + text
                    );

                    BigDecimal price =
                            parsePrice(text);

                    if (isValidPrice(price)) {

                        System.out.println(
                                "✅ Önceki fiyat bulundu: "
                                        + price
                        );

                        return price;
                    }
                }

            } catch (Exception e) {

                System.out.println(
                        "Önceki fiyat selector hatası: "
                                + selector
                                + " -> "
                                + e.getMessage()
                );
            }
        }

        return null;
    }

    // =========================================================
    // DISCOUNT
    // =========================================================

    private String calculateDiscount(
            BigDecimal currentPrice,
            BigDecimal previousPrice) {

        if (currentPrice == null ||
                previousPrice == null) {

            return null;
        }

        if (previousPrice.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return null;
        }

        if (previousPrice.compareTo(
                currentPrice
        ) <= 0) {

            return "0.00";
        }

        BigDecimal discount =
                previousPrice
                        .subtract(currentPrice)
                        .divide(
                                previousPrice,
                                4,
                                RoundingMode.HALF_UP
                        )
                        .multiply(
                                BigDecimal.valueOf(100)
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        return discount.toString();
    }

    // =========================================================
    // PRICE PARSER
    // =========================================================

    private BigDecimal parsePrice(
            String priceText) {

        if (priceText == null ||
                priceText.isBlank()) {

            return null;
        }

        try {

            String clean =
                    priceText
                            .replaceAll(
                                    "[^0-9.,]",
                                    ""
                            )
                            .trim();

            if (clean.isBlank()) {
                return null;
            }

            /*
             * 1.288,81
             * ->
             * 1288.81
             */

            if (clean.contains(".") &&
                    clean.contains(",")) {

                clean =
                        clean
                                .replace(".", "")
                                .replace(",", ".");
            }

            /*
             * 1288,81
             * ->
             * 1288.81
             */

            else if (clean.contains(",")) {

                clean =
                        clean.replace(
                                ",",
                                "."
                        );
            }

            return new BigDecimal(clean);

        } catch (Exception e) {

            System.out.println(
                    "Hepsiburada fiyat parse edilemedi: "
                            + priceText
            );

            return null;
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private boolean isValidPrice(
            BigDecimal price) {

        return price != null &&
                price.compareTo(
                        BigDecimal.ZERO
                ) > 0;
    }
}

