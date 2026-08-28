
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
import java.time.Duration;
import java.util.List;

@Component
public class AmazonScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null &&
                url.toLowerCase().contains("amazon.");
    }

    /*
     * ============================================================
     * URL'DEN ÜRÜN BİLGİLERİNİ ÇEK
     * ============================================================
     */

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {

        WebDriver driver = null;

        String productName = "Bilinmeyen Amazon Ürünü";

        long driverInitMs = 0;
        long pageLoadMs = 0;
        long titleFindMs = 0;
        long priceFindMs = 0;

        try {

            /*
             * ====================================================
             * CHROME
             * ====================================================
             */

            long start = System.currentTimeMillis();

            ChromeOptions options = new ChromeOptions();

            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--lang=tr-TR");

            options.setPageLoadStrategy(
                    PageLoadStrategy.EAGER
            );

            driver = new ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(
                            Duration.ofSeconds(40)
                    );

            driverInitMs =
                    System.currentTimeMillis() - start;


            /*
             * ====================================================
             * URL TEMİZLE
             * ====================================================
             */

            String cleanUrl =
                    cleanAmazonUrl(productUrl);

            System.out.println(
                    "Amazon URL: " + cleanUrl
            );


            /*
             * ====================================================
             * SAYFAYI AÇ
             * ====================================================
             */

            start = System.currentTimeMillis();

            try {
                driver.get(cleanUrl);
            } catch (Exception ignored) {
            }

            /*
             * Amazon fiyatlarının DOM'a gelmesini bekle.
             */

            Thread.sleep(5000);

            pageLoadMs =
                    System.currentTimeMillis() - start;


            System.out.println(
                    "Amazon Title: "
                            + driver.getTitle()
            );


            /*
             * ====================================================
             * ÜRÜN ADI
             * ====================================================
             */

            start = System.currentTimeMillis();

            productName =
                    findProductName(driver);

            titleFindMs =
                    System.currentTimeMillis() - start;


            /*
             * ====================================================
             * FİYATLAR
             * ====================================================
             */

            start = System.currentTimeMillis();

            BigDecimal currentPrice =
                    findCurrentPrice(driver);

            BigDecimal previousPrice =
                    findPreviousPrice(driver);


            /*
             * ====================================================
             * İNDİRİM HESAPLA
             * ====================================================
             */

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


            /*
             * ====================================================
             * DEBUG
             * ====================================================
             */

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Amazon Ürün: "
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


            /*
             * ====================================================
             * SONUÇ
             * ====================================================
             */

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


    /*
     * ============================================================
     * ÜRÜN ADI
     * ============================================================
     */

    private String findProductName(
            WebDriver driver) {

        /*
         * 1. Ana selector
         */

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.id("productTitle")
                    );

            if (!elements.isEmpty()) {

                String text =
                        elements.get(0)
                                .getText()
                                .trim();

                if (!text.isBlank()) {

                    System.out.println(
                            "Ürün adı bulundu: "
                                    + text
                    );

                    return text;
                }
            }

        } catch (Exception ignored) {
        }


        /*
         * 2. Yedek selector
         */

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    "#title span"
                            )
                    );

            if (!elements.isEmpty()) {

                String text =
                        elements.get(0)
                                .getText()
                                .trim();

                if (!text.isBlank()) {
                    return text;
                }
            }

        } catch (Exception ignored) {
        }


        return "Bilinmeyen Amazon Ürünü";
    }


    /*
     * ============================================================
     * GÜNCEL FİYAT
     * ============================================================
     *
     * SENİN HTML:
     *
     * <span class="a-price-whole">
     *     12.112
     * </span>
     *
     * <span class="a-price-fraction">
     *     50
     * </span>
     *
     * SONUÇ:
     *
     * 12112.50
     *
     * ============================================================
     */

    private BigDecimal findCurrentPrice(
            WebDriver driver) {

        /*
         * --------------------------------------------------------
         * 1. EN NET SELECTOR
         * --------------------------------------------------------
         */

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


                    System.out.println(
                            "Güncel fiyat whole: "
                                    + whole
                    );

                    System.out.println(
                            "Güncel fiyat fraction: "
                                    + fraction
                    );


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
         * --------------------------------------------------------
         * 2. YEDEK
         *
         * corePriceDisplay içindeki a-price-whole
         * --------------------------------------------------------
         */

        try {

            List<WebElement> wholeElements =
                    driver.findElements(
                            By.cssSelector(
                                    "#corePriceDisplay_desktop_feature_div " +
                                            ".a-price-whole"
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
                                    By.xpath(
                                            ".."
                                    )
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


    /*
     * ============================================================
     * ÖNCEKİ FİYAT
     * ============================================================
     *
     * SENİN HTML:
     *
     * <span class="a-price a-text-price
     *        apex-basisprice-value">
     *
     *     <span class="a-offscreen">
     *         14.250,00TL
     *     </span>
     *
     * </span>
     *
     * ============================================================
     */

    private BigDecimal findPreviousPrice(
            WebDriver driver) {

        /*
         * --------------------------------------------------------
         * EXACT SELECTOR
         * --------------------------------------------------------
         */

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

                System.out.println(
                        "Önceki fiyat adayı: "
                                + text
                );


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
         * --------------------------------------------------------
         * YEDEK
         * --------------------------------------------------------
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


    /*
     * ============================================================
     * GÜNCEL FİYAT PARSE
     * ============================================================
     *
     * whole    = 12.112
     * fraction = 50
     *
     * SONUÇ = 12112.50
     * ============================================================
     */

    private BigDecimal parseCurrentPrice(
            String whole,
            String fraction) {

        if (whole == null ||
                whole.isBlank()) {

            return null;
        }

        try {

            /*
             * 12.112
             * ↓
             * 12112
             */

            String cleanWhole =
                    whole.replaceAll(
                            "[^0-9]",
                            ""
                    );

            if (cleanWhole.isBlank()) {
                return null;
            }


            /*
             * Kuruş kısmı
             */

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


            /*
             * 5 -> 50
             */

            if (cleanFraction.length() == 1) {
                cleanFraction += "0";
            }

            /*
             * 500 gibi bir durum gelirse
             * ilk iki haneyi al.
             */

            if (cleanFraction.length() > 2) {

                cleanFraction =
                        cleanFraction.substring(
                                0,
                                2
                        );
            }


            String finalPrice =
                    cleanWhole
                            + "."
                            + cleanFraction;

            return new BigDecimal(finalPrice);

        } catch (Exception e) {

            return null;
        }
    }


    /*
     * ============================================================
     * TÜRKÇE FİYAT PARSE
     * ============================================================
     *
     * 14.250,00TL
     *
     * ↓
     *
     * 14250.00
     *
     * ============================================================
     */

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

                /*
                 * 14.250,00
                 *
                 * → 14250.00
                 */

                clean =
                        clean
                                .replace(
                                        ".",
                                        ""
                                )
                                .replace(
                                        ",",
                                        "."
                                );

            } else if (clean.contains(",")) {

                clean =
                        clean.replace(
                                ",",
                                "."
                        );
            }


            return new BigDecimal(clean);

        } catch (Exception e) {

            return null;
        }
    }


    /*
     * ============================================================
     * FİYAT VALIDATION
     * ============================================================
     */

    private boolean isValidPrice(
            BigDecimal price) {

        return price != null &&
                price.compareTo(
                        BigDecimal.ZERO
                ) > 0;
    }


    /*
     * ============================================================
     * AMAZON URL TEMİZLE
     * ============================================================
     *
     * /dp/B0GT2SD8XS/...
     *
     * ↓
     *
     * /dp/B0GT2SD8XS
     *
     * ============================================================
     */

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


        /*
         * /dp/ yoksa query parametrelerini
         * kaldır.
         */

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


    /*
     * ============================================================
     * ARAMA KULLANILMIYOR
     * ============================================================
     */

    @Override
    public ProductPriceInfo searchAndGetProduct(
            String productName) {

        return new ProductPriceInfo(
                "Amazon araması bu işlemde kullanılmıyor",
                null,
                null,
                null,
                "Amazon",
                null,
                0,
                0,
                0,
                0
        );
    }
}

