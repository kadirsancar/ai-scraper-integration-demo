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
public class TrendyolScraper implements ECommerceScraper {

    private static final String BASE_URL = "https://www.trendyol.com";

    @Override
    public boolean supports(String url) {
        return url != null
                && url.toLowerCase().contains("trendyol.com");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        return fetchProductDetailsInternal(productUrl);
    }

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {

        String searchUrl = BASE_URL + "/sr?q="
                + productName.replace(" ", "+");

        WebDriver driver = null;

        try {

            long start = System.currentTimeMillis();

            driver = new ChromeDriver(createChromeOptions());

            driver.get(searchUrl);

            WebDriverWait wait =
                    new WebDriverWait(driver, Duration.ofSeconds(15));

            /*
             * Trendyol arama sonuçları
             */
            String[] productSelectors = {

                    "div.p-card-wrppr a",
                    "a.p-card-chldrn-cntnr",
                    "a[href*='-p-']",
                    "div.product-card a"
            };

            WebElement firstProduct = null;

            for (String selector : productSelectors) {

                try {

                    firstProduct = wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    By.cssSelector(selector)
                            )
                    );

                    if (firstProduct != null) {
                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            if (firstProduct == null) {

                return new ProductPriceInfo(
                        "Trendyol arama sonucu bulunamadı",
                        null,
                        null,
                        null,
                        "Trendyol",
                        searchUrl,
                        0,
                        System.currentTimeMillis() - start,
                        0,
                        0
                );
            }

            String href = firstProduct.getAttribute("href");

            if (href == null || href.isBlank()) {

                return new ProductPriceInfo(
                        "Trendyol ürün URL'si bulunamadı",
                        null,
                        null,
                        null,
                        "Trendyol",
                        searchUrl,
                        0,
                        System.currentTimeMillis() - start,
                        0,
                        0
                );
            }

            String productUrl = href.startsWith("http")
                    ? href
                    : BASE_URL + href;

            System.out.println(
                    "Trendyol arama sonucu ürün URL: "
                            + productUrl
            );

            return getProductDetails(productUrl);

        } catch (Exception e) {

            return new ProductPriceInfo(
                    "Trendyol arama hatası: " + e.getMessage(),
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

    private ProductPriceInfo fetchProductDetailsInternal(
            String productUrl
    ) {

        WebDriver driver = null;

        String productName = "Bilinmeyen Trendyol Ürünü";

        BigDecimal currentPrice = null;
        BigDecimal originalPrice = null;

        String discountInfo = null;

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            // =====================================================
            // 1. CHROME BAŞLAT
            // =====================================================

            long stepStart = System.currentTimeMillis();

            ChromeOptions options = createChromeOptions();

            driver = new ChromeDriver(options);

            driverInitMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // 2. ÜRÜN SAYFASINI AÇ
            // =====================================================

            stepStart = System.currentTimeMillis();

            try {

                driver.get(productUrl);

            } catch (Exception e) {

                System.out.println(
                        "Sayfa yükleme uyarısı: "
                                + e.getMessage()
                );
            }

            pageLoadMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // 3. SAYFAYI BEKLE
            // =====================================================

            Thread.sleep(3000);


            // =====================================================
            // 4. ÜRÜN ADI
            // =====================================================

            stepStart = System.currentTimeMillis();

            String[] titleSelectors = {

                    "[data-testid='product-title']",
                    "h1[data-testid='product-title']",
                    ".pr-new-br",
                    "[data-testid='product-name']",
                    "h1"
            };

            for (String selector : titleSelectors) {

                try {

                    WebElement element =
                            driver.findElement(
                                    By.cssSelector(selector)
                            );

                    String text = element.getText();

                    if (text != null && !text.isBlank()) {

                        productName = text.trim();

                        System.out.println(
                                "Trendyol ürün adı bulundu ["
                                        + selector
                                        + "]: "
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
            // 5. FİYATLARI BUL
            // =====================================================

            stepStart = System.currentTimeMillis();

            WebDriverWait wait =
                    new WebDriverWait(
                            driver,
                            Duration.ofSeconds(10)
                    );


            // =====================================================
            // GÜNCEL FİYAT
            // =====================================================

            String[] currentPriceSelectors = {

                    /*
                     * Yeni Trendyol fiyat yapıları
                     */
                    "[data-testid='price-current']",
                    "[data-testid='product-price']",

                    /*
                     * Sepette fiyat
                     */
                    ".ty-plus-price-discounted-price",

                    /*
                     * Klasik Trendyol
                     */
                    ".prc-dsc",

                    /*
                     * Diğer fiyat yapıları
                     */
                    "p.new-price",
                    ".product-price-container .prc-dsc",
                    "[class*='discounted-price']",
                    "[class*='discountedPrice']",
                    "[class*='current-price']",
                    "[class*='currentPrice']"
            };

            for (String selector : currentPriceSelectors) {

                try {

                    List<WebElement> elements =
                            driver.findElements(
                                    By.cssSelector(selector)
                            );

                    for (WebElement element : elements) {

                        String priceText =
                                element.getText();

                        if (priceText == null
                                || priceText.isBlank()) {
                            continue;
                        }

                        System.out.println(
                                "Güncel fiyat adayı ["
                                        + selector
                                        + "]: "
                                        + priceText
                        );

                        BigDecimal parsed =
                                parsePrice(priceText);

                        if (parsed != null) {

                            currentPrice = parsed;

                            System.out.println(
                                    "Güncel fiyat bulundu: "
                                            + currentPrice
                                            + " TL"
                            );

                            break;
                        }
                    }

                    if (currentPrice != null) {
                        break;
                    }

                } catch (Exception ignored) {
                }
            }


            // =====================================================
            // ORİJİNAL FİYAT
            // =====================================================

            String[] originalPriceSelectors = {

                    /*
                     * Yeni Trendyol
                     */
                    "[data-testid='price-original']",
                    "[data-testid='original-price']",

                    /*
                     * Sepette indirim öncesi
                     */
                    ".ty-plus-price-original-price",

                    /*
                     * Klasik Trendyol
                     */
                    ".prc-slg",

                    "p.old-price",

                    /*
                     * Alternatifler
                     */
                    "[class*='original-price']",
                    "[class*='originalPrice']",
                    "[class*='old-price']",
                    "[class*='oldPrice']"
            };

            for (String selector : originalPriceSelectors) {

                try {

                    List<WebElement> elements =
                            driver.findElements(
                                    By.cssSelector(selector)
                            );

                    for (WebElement element : elements) {

                        String priceText =
                                element.getText();

                        if (priceText == null
                                || priceText.isBlank()) {
                            continue;
                        }

                        System.out.println(
                                "Orijinal fiyat adayı ["
                                        + selector
                                        + "]: "
                                        + priceText
                        );

                        BigDecimal parsed =
                                parsePrice(priceText);

                        if (parsed != null) {

                            originalPrice = parsed;

                            System.out.println(
                                    "Orijinal fiyat bulundu: "
                                            + originalPrice
                                            + " TL"
                            );

                            break;
                        }
                    }

                    if (originalPrice != null) {
                        break;
                    }

                } catch (Exception ignored) {
                }
            }


            // =====================================================
            // 6. JSON-LD FALLBACK
            // =====================================================

            if (currentPrice == null) {

                System.out.println(
                        "Selector ile fiyat bulunamadı. "
                                + "JSON-LD kontrol ediliyor."
                );

                try {

                    List<WebElement> scripts =
                            driver.findElements(
                                    By.cssSelector(
                                            "script[type='application/ld+json']"
                                    )
                            );

                    for (WebElement script : scripts) {

                        String json = script.getAttribute(
                                "textContent"
                        );

                        if (json == null || json.isBlank()) {
                            continue;
                        }

                        /*
                         * JSON-LD içerisindeki
                         * "price":"1234.90"
                         */
                        int priceIndex =
                                json.indexOf("\"price\"");

                        if (priceIndex >= 0) {

                            String section =
                                    json.substring(priceIndex);

                            int colonIndex =
                                    section.indexOf(":");

                            if (colonIndex >= 0) {

                                String value =
                                        section.substring(
                                                        colonIndex + 1
                                                )
                                                .split("[,}]")[0]
                                                .replace(
                                                        "\"",
                                                        ""
                                                )
                                                .trim();

                                try {

                                    BigDecimal parsed =
                                            new BigDecimal(value);

                                    if (parsed.compareTo(
                                            new BigDecimal("20")
                                    ) >= 0) {

                                        currentPrice = parsed;

                                        System.out.println(
                                                "JSON-LD güncel fiyat: "
                                                        + currentPrice
                                        );

                                        break;
                                    }

                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                } catch (Exception e) {

                    System.out.println(
                            "JSON-LD fiyat okunamadı: "
                                    + e.getMessage()
                    );
                }
            }


            // =====================================================
            // 7. İNDİRİM HESAPLA
            // =====================================================

            if (currentPrice != null
                    && originalPrice != null) {

                if (originalPrice.compareTo(
                        currentPrice
                ) > 0) {

                    BigDecimal difference =
                            originalPrice.subtract(
                                    currentPrice
                            );

                    BigDecimal percentage =
                            difference
                                    .divide(
                                            originalPrice,
                                            4,
                                            RoundingMode.HALF_UP
                                    )
                                    .multiply(
                                            new BigDecimal("100")
                                    )
                                    .setScale(
                                            2,
                                            RoundingMode.HALF_UP
                                    );

                    discountInfo =
                            "%"
                                    + percentage
                                    .stripTrailingZeros()
                                    .toPlainString()
                                    + " İndirim ("
                                    + difference
                                    .stripTrailingZeros()
                                    .toPlainString()
                                    + " TL Kazanç)";

                    System.out.println(
                            "Trendyol indirim: "
                                    + discountInfo
                    );

                } else {

                    /*
                     * Orijinal fiyat güncel fiyattan
                     * küçük/eşitse gerçek indirim yok.
                     */
                    originalPrice = currentPrice;
                }

            } else if (currentPrice != null) {

                /*
                 * Eski fiyat yoksa current price
                 * referans fiyat olarak tutuluyor.
                 */
                originalPrice = currentPrice;
            }


            priceFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // 8. SONUÇ
            // =====================================================

            return new ProductPriceInfo(

                    productName,

                    currentPrice,

                    originalPrice,

                    discountInfo,

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

                    "Trendyol işlem kesintiye uğradı",

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

                    "Trendyol hata: "
                            + e.getMessage(),

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
                "--disable-gpu"
        );

        options.addArguments(
                "--no-sandbox"
        );

        options.addArguments(
                "--disable-dev-shm-usage"
        );

        options.addArguments(
                "--disable-blink-features=AutomationControlled"
        );

        options.setExperimentalOption(
                "excludeSwitches",
                new String[]{
                        "enable-automation"
                }
        );

        options.addArguments(
                "user-agent=Mozilla/5.0 " +
                        "(Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        options.addArguments(
                "--window-size=1920,1080"
        );

        options.setPageLoadStrategy(
                PageLoadStrategy.EAGER
        );

        return options;
    }


    // =========================================================
    // PRICE PARSER
    // =========================================================

    private BigDecimal parsePrice(String priceText) {

        if (priceText == null
                || priceText.isBlank()) {

            return null;
        }

        try {

            String clean =
                    priceText.replaceAll(
                            "[^0-9.,]",
                            ""
                    );

            if (clean.isBlank()) {
                return null;
            }

            /*
             * 1.299,90
             * -> 1299.90
             */
            if (clean.contains(".")
                    && clean.contains(",")) {

                clean = clean
                        .replace(".", "")
                        .replace(",", ".");

            }

            /*
             * 408,40
             * -> 408.40
             */
            else if (clean.contains(",")) {

                clean = clean.replace(",", ".");
            }

            /*
             * 1299.90
             * -> 1299.90
             */
            BigDecimal parsed =
                    new BigDecimal(clean);

            /*
             * Çok küçük sayıları fiyat kabul etme.
             */
            if (parsed.compareTo(
                    new BigDecimal("20")
            ) < 0) {

                return null;
            }

            return parsed;

        } catch (Exception e) {

            System.out.println(
                    "Trendyol fiyat parse edilemedi: "
                            + priceText
            );

            return null;
        }
    }
}

