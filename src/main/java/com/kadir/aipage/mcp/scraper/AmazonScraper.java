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
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class AmazonScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null &&
                url.toLowerCase().contains("amazon.");
    }

    /**
     * Ürün adıyla Amazon'da arama yapar.
     *
     * İlk çıkan ürünü direkt seçer.
     * Sonuçlar arasında doğru ürünü aramaz.
     */
    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {

        WebDriver driver = null;

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            System.out.println("======================================");
            System.out.println(
                    "Amazon ürün araması: " + productName
            );
            System.out.println("======================================");

            long start = System.currentTimeMillis();

            ChromeOptions options = new ChromeOptions();

            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--lang=tr-TR");

            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            driver = new ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(Duration.ofSeconds(40));

            driverInitMs =
                    System.currentTimeMillis() - start;

            String encodedProduct =
                    URLEncoder.encode(
                            productName,
                            StandardCharsets.UTF_8
                    );

            String searchUrl =
                    "https://www.amazon.com.tr/s?k="
                            + encodedProduct;

            System.out.println(
                    "Amazon arama URL: " + searchUrl
            );

            start = System.currentTimeMillis();

            try {
                driver.get(searchUrl);
            } catch (Exception ignored) {
            }

            Thread.sleep(5000);

            pageLoadMs =
                    System.currentTimeMillis() - start;

            /*
             * =====================================================
             * İLK ÜRÜNÜ BUL
             * =====================================================
             *
             * Gönderdiğin HTML:
             *
             * <span data-component-type="s-product-image">
             *     <a href="/.../dp/B0DCNLS913/...">
             *         <img class="s-image"
             *              alt="Casio GR-B300-1A4DR ...">
             *
             * Buradan ilk "s-product-image" elementini alıyoruz.
             */

            List<WebElement> productImages =
                    driver.findElements(
                            By.cssSelector(
                                    "span[data-component-type='s-product-image'] a"
                            )
                    );

            System.out.println(
                    "Amazon arama sonuç sayısı: "
                            + productImages.size()
            );

            if (productImages.isEmpty()) {

                /*
                 * Alternatif selector.
                 * Amazon HTML yapısı değişirse bunu deniyoruz.
                 */
                productImages =
                        driver.findElements(
                                By.cssSelector(
                                        "a.a-link-normal.s-no-outline"
                                )
                        );

                System.out.println(
                        "Amazon yedek sonuç sayısı: "
                                + productImages.size()
                );
            }

            if (productImages.isEmpty()) {

                System.out.println(
                        "❌ Amazon'da arama sonucu bulunamadı."
                );

                return new ProductPriceInfo(
                        productName,
                        null,
                        null,
                        null,
                        "Amazon",
                        searchUrl,
                        driverInitMs,
                        pageLoadMs,
                        titleFindMs,
                        priceFindMs
                );
            }

            /*
             * =====================================================
             * İLK ÜRÜN
             * =====================================================
             */

            WebElement firstProduct =
                    productImages.get(0);

            String productUrl =
                    firstProduct.getAttribute("href");

            /*
             * Amazon bazen relative URL döndürür:
             *
             * /Casio-GR-B300-1A4-Erkek-Siyah-Silikon/dp/B0DCNLS913/...
             *
             * Bunu tam URL'ye çeviriyoruz.
             */

            if (productUrl != null &&
                    productUrl.startsWith("/")) {

                productUrl =
                        "https://www.amazon.com.tr"
                                + productUrl;
            }

            System.out.println(
                    "Amazon ilk ürün URL: "
                            + productUrl
            );

            /*
             * =====================================================
             * ÜRÜN SAYFASINA GİT
             * =====================================================
             */

            if (productUrl == null ||
                    productUrl.isBlank()) {

                System.out.println(
                        "❌ İlk ürünün URL'si alınamadı."
                );

                return new ProductPriceInfo(
                        productName,
                        null,
                        null,
                        null,
                        "Amazon",
                        searchUrl,
                        driverInitMs,
                        pageLoadMs,
                        titleFindMs,
                        priceFindMs
                );
            }

            /*
             * Arama sayfasındaki driver'ı kullanmaya devam ediyoruz.
             * Yeni Chrome açmıyoruz.
             */

            start = System.currentTimeMillis();

            try {
                driver.get(
                        cleanAmazonUrl(productUrl)
                );
            } catch (Exception ignored) {
            }

            Thread.sleep(5000);

            pageLoadMs +=
                    System.currentTimeMillis() - start;

            System.out.println(
                    "Amazon ürün sayfası: "
                            + driver.getTitle()
            );

            /*
             * =====================================================
             * ÜRÜN ADI
             * =====================================================
             */

            start = System.currentTimeMillis();

            String realProductName =
                    findProductName(driver);

            titleFindMs =
                    System.currentTimeMillis() - start;

            if (realProductName == null ||
                    realProductName.isBlank()) {

                realProductName = productName;
            }

            /*
             * =====================================================
             * FİYATLAR
             * =====================================================
             */

            start = System.currentTimeMillis();

            BigDecimal currentPrice =
                    findCurrentPrice(driver);

            BigDecimal previousPrice =
                    findPreviousPrice(driver);

            String discountPercentage = null;

            if (currentPrice != null &&
                    previousPrice != null &&
                    previousPrice.compareTo(
                            BigDecimal.ZERO
                    ) > 0 &&
                    previousPrice.compareTo(
                            currentPrice
                    ) > 0) {

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

                discountPercentage =
                        discount.toString();
            }

            priceFindMs =
                    System.currentTimeMillis() - start;

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Amazon Ürün: "
                            + realProductName
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
                    realProductName,
                    currentPrice,
                    previousPrice,
                    discountPercentage,
                    "Amazon",
                    cleanAmazonUrl(productUrl),
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );

        } catch (Exception e) {

            System.out.println(
                    "❌ Amazon scraper hatası: "
                            + e.getMessage()
            );

            e.printStackTrace();

            return new ProductPriceInfo(
                    productName,
                    null,
                    null,
                    null,
                    "Amazon",
                    null,
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


    /**
     * Amazon ürün detaylarını direkt ürün URL'sinden alır.
     */
    @Override
    public ProductPriceInfo getProductDetails(
            String productUrl) {

        WebDriver driver = null;

        String productName =
                "Bilinmeyen Amazon Ürünü";

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            long start =
                    System.currentTimeMillis();

            ChromeOptions options =
                    new ChromeOptions();

            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--lang=tr-TR");

            options.setPageLoadStrategy(
                    PageLoadStrategy.EAGER
            );

            driver =
                    new ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(
                            Duration.ofSeconds(40)
                    );

            driverInitMs =
                    System.currentTimeMillis()
                            - start;

            String cleanUrl =
                    cleanAmazonUrl(productUrl);

            System.out.println(
                    "Amazon URL: "
                            + cleanUrl
            );

            start =
                    System.currentTimeMillis();

            try {
                driver.get(cleanUrl);
            } catch (Exception ignored) {
            }

            Thread.sleep(5000);

            pageLoadMs =
                    System.currentTimeMillis()
                            - start;

            start =
                    System.currentTimeMillis();

            productName =
                    findProductName(driver);

            titleFindMs =
                    System.currentTimeMillis()
                            - start;

            start =
                    System.currentTimeMillis();

            BigDecimal currentPrice =
                    findCurrentPrice(driver);

            BigDecimal previousPrice =
                    findPreviousPrice(driver);

            String discountPercentage = null;

            if (currentPrice != null &&
                    previousPrice != null &&
                    previousPrice.compareTo(
                            BigDecimal.ZERO
                    ) > 0 &&
                    previousPrice.compareTo(
                            currentPrice
                    ) > 0) {

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

                discountPercentage =
                        discount.toString();
            }

            priceFindMs =
                    System.currentTimeMillis()
                            - start;

            return new ProductPriceInfo(
                    productName,
                    currentPrice,
                    previousPrice,
                    discountPercentage,
                    "Amazon",
                    cleanUrl,
                    driverInitMs,
                    pageLoadMs,
                    titleFindMs,
                    priceFindMs
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new ProductPriceInfo(
                    productName,
                    null,
                    null,
                    null,
                    "Amazon",
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


    /**
     * Ürün sayfasındaki başlığı bulur.
     */
    private String findProductName(
            WebDriver driver) {

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.id("productTitle")
                    );

            if (!elements.isEmpty()) {

                String title =
                        elements.get(0)
                                .getText()
                                .trim();

                if (!title.isBlank()) {
                    return title;
                }
            }

        } catch (Exception ignored) {
        }

        return "Bilinmeyen Amazon Ürünü";
    }


    /**
     * Amazon güncel fiyatını bulur.
     */
    private BigDecimal findCurrentPrice(
            WebDriver driver) {

        try {

            List<WebElement> priceContainers =
                    driver.findElements(
                            By.cssSelector(
                                    ".apex-pricetopay-value"
                            )
                    );

            System.out.println(
                    "Güncel fiyat container sayısı: "
                            + priceContainers.size()
            );

            for (WebElement container :
                    priceContainers) {

                try {

                    WebElement wholeElement =
                            container.findElement(
                                    By.cssSelector(
                                            ".a-price-whole"
                                    )
                            );

                    String whole =
                            wholeElement
                                    .getText()
                                    .trim();

                    String fraction = "00";

                    try {

                        WebElement fractionElement =
                                container.findElement(
                                        By.cssSelector(
                                                ".a-price-fraction"
                                        )
                                );

                        fraction =
                                fractionElement
                                        .getText()
                                        .trim();

                    } catch (Exception ignored) {
                    }

                    BigDecimal price =
                            parseCurrentPrice(
                                    whole,
                                    fraction
                            );

                    if (isValidPrice(price)) {

                        System.out.println(
                                "Güncel fiyat bulundu: "
                                        + price
                        );

                        return price;
                    }

                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Güncel fiyat container hatası: "
                            + e.getMessage()
            );
        }

        /*
         * YEDEK SELECTOR
         */

        try {

            List<WebElement> wholeElements =
                    driver.findElements(
                            By.cssSelector(
                                    "#corePriceDisplay_desktop_feature_div "
                                            + ".a-price-whole"
                            )
                    );

            System.out.println(
                    "Yedek whole element sayısı: "
                            + wholeElements.size()
            );

            for (WebElement wholeElement :
                    wholeElements) {

                String whole =
                        wholeElement
                                .getText()
                                .trim();

                String fraction = "00";

                try {

                    WebElement parent =
                            wholeElement.findElement(
                                    By.xpath("..")
                            );

                    WebElement fractionElement =
                            parent.findElement(
                                    By.cssSelector(
                                            ".a-price-fraction"
                                    )
                            );

                    fraction =
                            fractionElement
                                    .getText()
                                    .trim();

                } catch (Exception ignored) {
                }

                BigDecimal price =
                        parseCurrentPrice(
                                whole,
                                fraction
                        );

                if (isValidPrice(price)) {
                    return price;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Yedek güncel fiyat hatası: "
                            + e.getMessage()
            );
        }

        return null;
    }


    /**
     * Amazon üzerindeki eski / üstü çizili fiyatı bulur.
     */
    private BigDecimal findPreviousPrice(
            WebDriver driver) {

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    ".apex-basisprice-value .a-offscreen"
                            )
                    );

            System.out.println(
                    "Önceki fiyat element sayısı: "
                            + elements.size()
            );

            for (WebElement element :
                    elements) {

                String text =
                        element
                                .getAttribute(
                                        "textContent"
                                )
                                .trim();

                BigDecimal price =
                        parseTurkishPrice(text);

                if (isValidPrice(price)) {

                    System.out.println(
                            "Önceki fiyat bulundu: "
                                    + price
                    );

                    return price;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Önceki fiyat hatası: "
                            + e.getMessage()
            );
        }

        /*
         * YEDEK SELECTOR
         */

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    ".basisPrice .a-offscreen"
                            )
                    );

            for (WebElement element :
                    elements) {

                String text =
                        element
                                .getAttribute(
                                        "textContent"
                                )
                                .trim();

                BigDecimal price =
                        parseTurkishPrice(text);

                if (isValidPrice(price)) {
                    return price;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }


    private BigDecimal parseCurrentPrice(
            String whole,
            String fraction) {

        if (whole == null ||
                whole.isBlank()) {
            return null;
        }

        try {

            String cleanWhole =
                    whole.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (cleanWhole.isBlank()) {
                return null;
            }

            String cleanFraction =
                    fraction == null
                            ? "00"
                            : fraction.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (cleanFraction.isBlank()) {
                cleanFraction = "00";
            }

            if (cleanFraction.length() == 1) {
                cleanFraction += "0";
            }

            if (cleanFraction.length() > 2) {

                cleanFraction =
                        cleanFraction.substring(
                                0,
                                2
                        );
            }

            return new BigDecimal(
                    cleanWhole
                            + "."
                            + cleanFraction
            );

        } catch (Exception e) {
            return null;
        }
    }


    private BigDecimal parseTurkishPrice(
            String text) {

        if (text == null ||
                text.isBlank()) {
            return null;
        }

        try {

            String clean =
                    text
                            .replaceAll(
                                    "[^0-9.,]",
                                    ""
                            )
                            .trim();

            if (clean.isBlank()) {
                return null;
            }

            if (clean.contains(".") &&
                    clean.contains(",")) {

                clean =
                        clean
                                .replace(".", "")
                                .replace(",", ".");

            } else if (clean.contains(",")) {

                clean =
                        clean.replace(",", ".");
            }

            return new BigDecimal(clean);

        } catch (Exception e) {
            return null;
        }
    }


    private boolean isValidPrice(
            BigDecimal price) {

        return price != null &&
                price.compareTo(
                        BigDecimal.ZERO
                ) > 0;
    }


    private String cleanAmazonUrl(
            String url) {

        if (url == null ||
                url.isBlank()) {
            return url;
        }

        try {

            int dpIndex =
                    url.indexOf("/dp/");

            if (dpIndex != -1) {

                int asinStart =
                        dpIndex + 4;

                int asinEnd =
                        url.indexOf(
                                "/",
                                asinStart
                        );

                if (asinEnd == -1) {

                    asinEnd =
                            url.indexOf(
                                    "?",
                                    asinStart
                            );
                }

                if (asinEnd == -1) {
                    asinEnd = url.length();
                }

                String asin =
                        url.substring(
                                asinStart,
                                asinEnd
                        );

                return "https://www.amazon.com.tr/dp/"
                        + asin;
            }

        } catch (Exception ignored) {
        }

        int questionIndex =
                url.indexOf("?");

        if (questionIndex != -1) {

            return url.substring(
                    0,
                    questionIndex
            );
        }

        return url;
    }
}

