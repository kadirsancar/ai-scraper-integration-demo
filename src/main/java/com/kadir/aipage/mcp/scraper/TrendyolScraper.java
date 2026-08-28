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
            ChromeOptions options = createChromeOptions();

            driver = new ChromeDriver(options);
            driver.get(searchUrl);

            WebDriverWait wait = new WebDriverWait(
                    driver,
                    Duration.ofSeconds(10)
            );

            WebElement firstProduct = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("div.p-card-wrppr a")
                    )
            );

            String href = firstProduct.getAttribute("href");

            String productUrl = href.startsWith("http")
                    ? href
                    : "https://www.trendyol.com" + href;

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

            driverInitMs = System.currentTimeMillis() - stepStart;


            // =====================================================
            // 2. ÜRÜN SAYFASINI AÇ
            // =====================================================

            stepStart = System.currentTimeMillis();

            try {
                driver.get(productUrl);
            } catch (Exception ignored) {
            }

            pageLoadMs = System.currentTimeMillis() - stepStart;


            // =====================================================
            // 3. SAYFANIN YÜKLENMESİNİ BEKLE
            // =====================================================

            Thread.sleep(3000);


            // =====================================================
            // 4. ÜRÜN ADINI BUL
            // =====================================================

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
                            driver.findElement(By.cssSelector(selector));

                    String text = titleElement.getText();

                    if (text != null && !text.isBlank()) {

                        productName = text.trim();

                        System.out.println(
                                "Ürün adı bulundu [" + selector + "]: "
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
                    new WebDriverWait(driver, Duration.ofSeconds(10));


            // =====================================================
            // GÜNCEL FİYAT
            // =====================================================

            String[] currentPriceSelectors = {

                    // Trendyol - Sepette fiyat
                    ".ty-plus-price-discounted-price",

                    // Alternatif Trendyol yapıları
                    ".prc-dsc",

                    "p.new-price",

                    "div.price-container span.discounted",

                    "[class*='discounted-price']",

                    "[class*='discountedPrice']",

                    "[class*='current-price']",

                    "[class*='currentPrice']"
            };

            for (String selector : currentPriceSelectors) {

                try {

                    WebElement priceElement = wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    By.cssSelector(selector)
                            )
                    );

                    String priceText = priceElement.getText();

                    System.out.println(
                            "Güncel fiyat adayı [" + selector + "]: "
                                    + priceText
                    );

                    currentPrice = parsePrice(priceText);

                    if (currentPrice != null) {

                        System.out.println(
                                "Güncel fiyat bulundu: "
                                        + currentPrice
                                        + " TL"
                        );

                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            if (currentPrice == null) {

                System.out.println(
                        "Güncel fiyat hiçbir selector ile bulunamadı."
                );
            }


            // =====================================================
            // ORİJİNAL FİYAT
            // =====================================================

            String[] originalPriceSelectors = {

                    // Trendyol - Sepette indirim öncesi fiyat
                    ".ty-plus-price-original-price",

                    // Alternatif Trendyol yapıları
                    ".prc-slg",

                    "p.old-price",

                    "[class*='original-price']",

                    "[class*='originalPrice']",

                    "[class*='old-price']"
            };

            for (String selector : originalPriceSelectors) {

                try {

                    WebElement priceElement =
                            driver.findElement(
                                    By.cssSelector(selector)
                            );

                    String priceText =
                            priceElement.getText();

                    System.out.println(
                            "Orijinal fiyat adayı ["
                                    + selector
                                    + "]: "
                                    + priceText
                    );

                    originalPrice =
                            parsePrice(priceText);

                    if (originalPrice != null) {

                        System.out.println(
                                "Orijinal fiyat bulundu: "
                                        + originalPrice
                                        + " TL"
                        );

                        break;
                    }

                } catch (Exception ignored) {
                }
            }

            if (originalPrice == null) {

                System.out.println(
                        "Orijinal fiyat bulunamadı."
                );
            }


            // =====================================================
            // 6. İNDİRİM HESAPLAMA
            // =====================================================

            if (currentPrice != null && originalPrice != null) {

                if (originalPrice.compareTo(currentPrice) > 0) {

                    BigDecimal diff =
                            originalPrice.subtract(currentPrice);

                    BigDecimal percentage =
                            diff.divide(
                                            originalPrice,
                                            4,
                                            RoundingMode.HALF_UP
                                    )
                                    .multiply(new BigDecimal("100"))
                                    .setScale(2, RoundingMode.HALF_UP);

                    discountInfo =
                            "%"
                                    + percentage
                                    .stripTrailingZeros()
                                    .toPlainString()
                                    + " İndirim ("
                                    + diff
                                    .stripTrailingZeros()
                                    .toPlainString()
                                    + " TL Kazanç)";

                    System.out.println(
                            "İndirim bilgisi: "
                                    + discountInfo
                    );

                } else {

                    originalPrice = currentPrice;
                }

            } else if (currentPrice != null) {

                originalPrice = currentPrice;
            }


            priceFindMs =
                    System.currentTimeMillis() - stepStart;


            // =====================================================
            // 7. SONUÇ
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

                    "İşlem kesintiye uğradı",

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


    // =========================================================
    // CHROME OPTIONS
    // =========================================================

    private ChromeOptions createChromeOptions() {

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless=new");

        options.addArguments("--disable-gpu");

        options.addArguments("--no-sandbox");

        options.addArguments("--disable-dev-shm-usage");

        options.addArguments(
                "--disable-blink-features=AutomationControlled"
        );

        options.setExperimentalOption(
                "excludeSwitches",
                new String[]{"enable-automation"}
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

        if (priceText == null || priceText.isBlank()) {
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


            // Örnek:
            // 1.299,90 -> 1299.90

            if (clean.contains(".")
                    && clean.contains(",")) {

                clean = clean
                        .replace(".", "")
                        .replace(",", ".");

            }

            // Örnek:
            // 408,40 -> 408.40

            else if (clean.contains(",")) {

                clean = clean.replace(",", ".");
            }


            BigDecimal parsed =
                    new BigDecimal(clean);


            // 20 TL altındaki değerleri fiyat olarak kabul etme

            if (parsed.compareTo(
                    new BigDecimal("20")
            ) < 0) {

                return null;
            }

            return parsed;


        } catch (Exception e) {

            System.out.println(
                    "Fiyat parse edilemedi: "
                            + priceText
            );

            return null;
        }
    }
}