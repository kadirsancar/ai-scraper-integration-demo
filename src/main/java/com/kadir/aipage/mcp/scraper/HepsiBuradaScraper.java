
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
public class HepsiBuradaScraper implements ECommerceScraper {

    @Override
    public boolean supports(String url) {
        return url != null &&
                url.toLowerCase().contains("hepsiburada.com");
    }

    @Override
    public ProductPriceInfo getProductDetails(String productUrl) {
        return fetchProductDetailsInternal(productUrl);
    }

    @Override
    public ProductPriceInfo searchAndGetProduct(String productName) {

        String searchUrl =
                "https://www.hepsiburada.com/ara?q="
                        + productName.replace(" ", "+");

        WebDriver driver = null;

        try {

            ChromeOptions options = createChromeOptions();

            driver = new ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(
                            Duration.ofSeconds(40)
                    );

            driver.get(searchUrl);

            Thread.sleep(5000);

            WebElement firstProduct =
                    driver.findElement(
                            By.cssSelector(
                                    "li[id^='product-item'] a"
                            )
                    );

            String productUrl =
                    firstProduct.getAttribute("href");

            if (productUrl != null &&
                    productUrl.startsWith("/")) {

                productUrl =
                        "https://www.hepsiburada.com"
                                + productUrl;
            }

            return getProductDetails(productUrl);

        } catch (Exception e) {

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


    /*
     * ============================================================
     * ANA SCRAPER
     * ============================================================
     */

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

            /*
             * ====================================================
             * CHROME
             * ====================================================
             */

            long start =
                    System.currentTimeMillis();

            ChromeOptions options =
                    createChromeOptions();

            driver =
                    new ChromeDriver(options);

            driver.manage()
                    .timeouts()
                    .pageLoadTimeout(
                            Duration.ofSeconds(40)
                    );

            driverInitMs =
                    System.currentTimeMillis() - start;


            /*
             * ====================================================
             * SAYFAYI AÇ
             * ====================================================
             */

            start =
                    System.currentTimeMillis();

            try {

                driver.get(productUrl);

            } catch (Exception ignored) {
            }

            /*
             * JS tamamen çalışsın.
             */

            Thread.sleep(6000);

            pageLoadMs =
                    System.currentTimeMillis() - start;


            /*
             * ====================================================
             * SAYFA KONTROLÜ
             * ====================================================
             */

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Hepsiburada URL: "
                            + productUrl
            );

            System.out.println(
                    "Hepsiburada Title: "
                            + driver.getTitle()
            );


            /*
             * Güvenlik sayfası mı?
             */

            if (isSecurityPage(driver)) {

                System.out.println(
                        "HEPSIBURADA GÜVENLİK SAYFASI ALGILANDI!"
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


            /*
             * ====================================================
             * ÜRÜN ADI
             * ====================================================
             */

            start =
                    System.currentTimeMillis();

            productName =
                    findProductName(driver);

            titleFindMs =
                    System.currentTimeMillis() - start;


            /*
             * ====================================================
             * FİYATLAR
             * ====================================================
             */

            start =
                    System.currentTimeMillis();

            BigDecimal currentPrice =
                    findCurrentPrice(driver);

            BigDecimal previousPrice =
                    findPreviousPrice(driver);


            /*
             * ====================================================
             * İNDİRİM
             * ====================================================
             */

            String discountPercentage =
                    calculateDiscount(
                            currentPrice,
                            previousPrice
                    );


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


    /*
     * ============================================================
     * CHROME AYARLARI
     * ============================================================
     */

    private ChromeOptions createChromeOptions() {

        ChromeOptions options =
                new ChromeOptions();

        /*
         * Headless yerine normal Chrome.
         *
         * Hepsiburada güvenlik sistemi headless
         * tarayıcıları daha kolay engelleyebiliyor.
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
                "--start-maximized"
        );

        options.setPageLoadStrategy(
                PageLoadStrategy.EAGER
        );

        return options;
    }


    /*
     * ============================================================
     * GÜVENLİK SAYFASI KONTROLÜ
     * ============================================================
     */

    private boolean isSecurityPage(
            WebDriver driver) {

        try {

            String title =
                    driver.getTitle();

            if (title != null &&
                    title.toLowerCase()
                            .contains("güvenlik")) {

                return true;
            }

            String body =
                    driver.findElement(
                            By.tagName("body")
                    ).getText();

            if (body != null) {

                String lower =
                        body.toLowerCase();

                if (lower.contains("güvenlik kontrolü") ||
                        lower.contains("güvenlik") &&
                                lower.contains("hepsiburada")) {

                    return true;
                }
            }

        } catch (Exception ignored) {
        }

        return false;
    }


    /*
     * ============================================================
     * ÜRÜN ADI
     * ============================================================
     */

    private String findProductName(
            WebDriver driver) {

        /*
         * 1
         */

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.id("product-name")
                    );

            for (WebElement element :
                    elements) {

                String text =
                        element.getText().trim();

                if (!text.isBlank()) {
                    return text;
                }
            }

        } catch (Exception ignored) {
        }


        /*
         * 2
         */

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    "[data-test-id='title']"
                            )
                    );

            for (WebElement element :
                    elements) {

                String text =
                        element.getText().trim();

                if (!text.isBlank()) {
                    return text;
                }
            }

        } catch (Exception ignored) {
        }


        /*
         * 3
         */

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
                        .trim();
            }

        } catch (Exception ignored) {
        }


        return "Bilinmeyen Hepsiburada Ürünü";
    }


    /*
     * ============================================================
     * GÜNCEL FİYAT
     * ============================================================
     *
     * HTML:
     *
     * data-test-id="default-price"
     *
     * 209,90 TL
     *
     * ============================================================
     */

    private BigDecimal findCurrentPrice(
            WebDriver driver) {

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    "[data-test-id='default-price'] span"
                            )
                    );

            System.out.println(
                    "Güncel fiyat element sayısı: "
                            + elements.size()
            );


            for (WebElement element :
                    elements) {

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
                            "Güncel fiyat bulundu: "
                                    + price
                    );

                    return price;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Güncel fiyat hatası: "
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
     * HTML:
     *
     * data-test-id="prev-price"
     *
     * 249,89 TL
     *
     * ============================================================
     */

    private BigDecimal findPreviousPrice(
            WebDriver driver) {

        try {

            List<WebElement> elements =
                    driver.findElements(
                            By.cssSelector(
                                    "[data-test-id='prev-price'] span"
                            )
                    );

            System.out.println(
                    "Önceki fiyat element sayısı: "
                            + elements.size()
            );


            for (WebElement element :
                    elements) {

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

        return null;
    }


    /*
     * ============================================================
     * İNDİRİM HESAPLA
     * ============================================================
     */

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


    /*
     * ============================================================
     * FİYAT PARSE
     * ============================================================
     */

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
             * 1.249,90
             *
             * ->
             *
             * 1249.90
             */

            if (clean.contains(".") &&
                    clean.contains(",")) {

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
     * VALIDATION
     * ============================================================
     */

    private boolean isValidPrice(
            BigDecimal price) {

        return price != null &&
                price.compareTo(
                        BigDecimal.ZERO
                ) > 0;
    }
}

