package com.kadir.aipage.mcp;

import com.kadir.aipage.dto.ProductPriceInfo;
import com.kadir.aipage.entity.ScraperLog;
import com.kadir.aipage.mcp.scraper.ECommerceScraper;
import com.kadir.aipage.repository.ScraperLogRepository;
import com.kadir.aipage.service.ProductTrackingService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ECommerceTool {

    private final List<ECommerceScraper> scrapers;
    private final ProductTrackingService productTrackingService;
    private final ScraperLogRepository scraperLogRepository;

    private final ExecutorService scraperExecutor =
            Executors.newFixedThreadPool(4);

    public ECommerceTool(
            List<ECommerceScraper> scrapers,
            ProductTrackingService productTrackingService,
            ScraperLogRepository scraperLogRepository) {

        this.scrapers = scrapers;
        this.productTrackingService = productTrackingService;
        this.scraperLogRepository = scraperLogRepository;
    }

    // =========================================================
    // URL İLE TEK ÜRÜN FİYATI
    // =========================================================

    public String getProductPrice(String productUrl) {

        if (productUrl == null || productUrl.isBlank()) {
            return "Geçersiz ürün URL'si.";
        }

        for (ECommerceScraper scraper : scrapers) {

            if (!scraper.supports(productUrl)) {
                continue;
            }

            String platformName =
                    scraper.getClass().getSimpleName();

            System.out.println(
                    "Uygun scraper bulundu: " + platformName
            );

            long startTime = System.currentTimeMillis();

            boolean success = false;
            String errorMessage = null;
            ProductPriceInfo info = null;

            try {

                info = scraper.getProductDetails(productUrl);

                if (info != null
                        && info.getCurrentPrice() != null) {

                    success = true;

                } else {

                    errorMessage =
                            info != null
                                    ? info.getProductName()
                                    : "Fiyat okunamadı";
                }

            } catch (Exception e) {

                errorMessage = e.getMessage();
            }

            long durationMs =
                    System.currentTimeMillis() - startTime;

            String formattedTime =
                    formatDuration(durationMs);

            System.out.println(
                    "⏱️ [" + platformName + "] Geçen Süre: "
                            + formattedTime
            );

            saveScraperLog(
                    platformName,
                    productUrl,
                    durationMs,
                    formattedTime,
                    info,
                    success,
                    errorMessage
            );

            if (!success) {

                return "Ürün bilgileri çekildi ancak fiyat okunamadı "
                        + "veya hata oluştu: "
                        + errorMessage;
            }

            productTrackingService.savePriceRecord(
                    info.getProductUrl(),
                    info.getPlatform(),
                    info.getProductName(),
                    info.getCurrentPrice(),
                    info.getOriginalPrice(),
                    info.getDiscountInfo()
            );

            /*
             * URL'yi de sonucu içinde taşıyoruz.
             * AiChatService bu URL'yi OpenRouter cevabından bağımsız
             * olarak kullanıcıya tekrar ekleyecek.
             */
            return String.format(
                    "Ürün: %s | Güncel Fiyat: %s TL | "
                            + "Önceki Fiyat: %s TL | İndirim: %s | "
                            + "Platform: %s | Süre: %s | "
                            + "PRODUCT_URL:%s",

                    info.getProductName(),
                    info.getCurrentPrice(),

                    info.getOriginalPrice() != null
                            ? info.getOriginalPrice()
                            : "-",

                    info.getDiscountInfo() != null
                            && !info.getDiscountInfo().isBlank()
                            ? info.getDiscountInfo()
                            : "-",

                    info.getPlatform(),
                    formattedTime,

                    info.getProductUrl() != null
                            ? info.getProductUrl()
                            : ""
            );
        }

        return "Bu URL adresini destekleyen herhangi "
                + "bir e-ticaret scraper bulunamadı.";
    }

    // =========================================================
    // ÜRÜN ADI İLE 4 SİTEDE PARALEL ARAMA
    // =========================================================

    public String analyzeProductByName(String productName) {

        if (productName == null || productName.isBlank()) {
            return "Geçersiz ürün adı.";
        }

        System.out.println(
                "🔎 Paralel çoklu platform araması başlatıldı: "
                        + productName
        );

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<ProductPriceInfo>> futures =
                new ArrayList<>();

        // =====================================================
        // HER SCRAPER AYRI THREAD ÜZERİNDE ÇALIŞIR
        // =====================================================

        for (ECommerceScraper scraper : scrapers) {

            CompletableFuture<ProductPriceInfo> future =
                    CompletableFuture.supplyAsync(
                            () -> {

                                String platformName =
                                        scraper.getClass()
                                                .getSimpleName();

                                System.out.println(
                                        "🔎 "
                                                + platformName
                                                + " üzerinden arama yapılıyor..."
                                );

                                try {

                                    ProductPriceInfo info =
                                            scraper.searchAndGetProduct(
                                                    productName
                                            );

                                    if (info != null
                                            && info.getCurrentPrice() != null) {

                                        System.out.println(
                                                "✅ "
                                                        + platformName
                                                        + " fiyat bulundu: "
                                                        + info.getCurrentPrice()
                                                        + " TL"
                                        );

                                        savePrice(info);

                                    } else {

                                        System.out.println(
                                                "❌ "
                                                        + platformName
                                                        + " ürün/fiyat bulunamadı."
                                        );
                                    }

                                    return info;

                                } catch (Exception e) {

                                    System.out.println(
                                            "❌ "
                                                    + platformName
                                                    + " hata: "
                                                    + e.getMessage()
                                    );

                                    return null;
                                }
                            },
                            scraperExecutor
                    );

            futures.add(future);
        }

        // =====================================================
        // TÜM SCRAPER'LARIN BİTMESİNİ BEKLE
        // =====================================================

        List<ProductPriceInfo> results = new ArrayList<>();

        for (CompletableFuture<ProductPriceInfo> future : futures) {

            try {

                ProductPriceInfo info = future.join();

                if (info != null
                        && info.getCurrentPrice() != null) {

                    results.add(info);
                }

            } catch (Exception e) {

                System.out.println(
                        "Scraper sonucu alınamadı: "
                                + e.getMessage()
                );
            }
        }

        long totalDuration =
                System.currentTimeMillis() - startTime;

        System.out.println(
                "⏱️ Tüm scraper'lar tamamlandı: "
                        + formatDuration(totalDuration)
        );

        if (results.isEmpty()) {

            return "Belirtilen ürün adı için "
                    + "hiçbir platformda geçerli fiyat "
                    + "bilgisine ulaşılamadı.";
        }

        // =====================================================
        // EN UCUZ FİYATI JAVA TARAFINDA BUL
        // =====================================================

        ProductPriceInfo cheapestProduct =
                results.get(0);

        for (ProductPriceInfo info : results) {

            if (info.getCurrentPrice()
                    .compareTo(
                            cheapestProduct.getCurrentPrice()
                    ) < 0) {

                cheapestProduct = info;
            }
        }

        // =====================================================
        // MARKDOWN TABLOSU
        // =====================================================

        StringBuilder result = new StringBuilder();

        result.append(
                "## 🛒 "
                        + productName
                        + " - Fiyat Karşılaştırması\n\n"
        );

        result.append(
                "| Site | Ürün | Fiyat | Eski Fiyat | İndirim |\n"
        );

        result.append(
                "|---|---|---:|---:|---:|\n"
        );

        for (ProductPriceInfo info : results) {

            String price =
                    formatPrice(
                            info.getCurrentPrice()
                    );

            String originalPrice =
                    info.getOriginalPrice() != null
                            ? formatPrice(
                            info.getOriginalPrice()
                    ) + " TL"
                            : "-";

            String discount =
                    info.getDiscountInfo() != null
                            && !info.getDiscountInfo().isBlank()
                            ? info.getDiscountInfo()
                            : "-";

            result.append(
                    "| "
                            + escapeMarkdown(
                            info.getPlatform()
                    )
                            + " | "
                            + escapeMarkdown(
                            info.getProductName()
                    )
                            + " | "
                            + price
                            + " TL | "
                            + originalPrice
                            + " | "
                            + escapeMarkdown(discount)
                            + " |\n"
            );
        }

        // =====================================================
        // EN UCUZ FİYAT
        // =====================================================

        String cheapestPrice =
                formatPrice(
                        cheapestProduct.getCurrentPrice()
                );

        result.append("\n");

        result.append(
                "### 💰 En Ucuz Fiyat\n\n"
        );

        result.append(
                "**"
                        + escapeMarkdown(
                        cheapestProduct.getPlatform()
                )
                        + "** - "
                        + cheapestPrice
                        + " TL\n"
        );

        result.append(
                "Ürün: "
                        + escapeMarkdown(
                        cheapestProduct.getProductName()
                )
                        + "\n"
        );

        /*
         * ÖNEMLİ:
         *
         * URL'yi Markdown link olarak vermiyoruz.
         *
         * PRODUCT_URL etiketi sayesinde AiChatService gerçek
         * scraper URL'sini OpenRouter cevabından bağımsız
         * şekilde alabiliyor.
         */
        if (cheapestProduct.getProductUrl() != null
                && !cheapestProduct.getProductUrl().isBlank()) {

            result.append(
                    "PRODUCT_URL:"
                            + cheapestProduct.getProductUrl()
                            + "\n"
            );
        }

        result.append(
                "Toplam tarama süresi: "
                        + formatDuration(totalDuration)
                        + "\n"
        );

        return result.toString();
    }

    // =========================================================
    // FİYAT KAYDI
    // =========================================================

    private void savePrice(ProductPriceInfo info) {

        productTrackingService.savePriceRecord(
                info.getProductUrl(),
                info.getPlatform(),
                info.getProductName(),
                info.getCurrentPrice(),
                info.getOriginalPrice(),
                info.getDiscountInfo()
        );
    }

    // =========================================================
    // SCRAPER LOG
    // =========================================================

    private void saveScraperLog(
            String platformName,
            String productUrl,
            long durationMs,
            String formattedTime,
            ProductPriceInfo info,
            boolean success,
            String errorMessage) {

        ScraperLog log = new ScraperLog();

        log.setPlatform(platformName);
        log.setProductUrl(productUrl);
        log.setTotalDurationMs(durationMs);
        log.setFormattedDuration(formattedTime);

        if (info != null) {

            log.setDriverInitDurationMs(
                    info.getDriverInitMs()
            );

            log.setPageLoadDurationMs(
                    info.getPageLoadMs()
            );

            log.setTitleFindDurationMs(
                    info.getTitleFindMs()
            );

            log.setPriceFindDurationMs(
                    info.getPriceFindMs()
            );
        }

        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());

        scraperLogRepository.save(log);
    }

    // =========================================================
    // FİYAT FORMATLAMA
    // =========================================================

    private String formatPrice(
            java.math.BigDecimal price) {

        if (price == null) {
            return "Yok";
        }

        return price.stripTrailingZeros()
                .toPlainString()
                .replace(".", ",");
    }

    // =========================================================
    // MARKDOWN KARAKTERLERİNİ ESCAPE ET
    // =========================================================

    private String escapeMarkdown(String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("|", "\\|")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    // =========================================================
    // SÜRE
    // =========================================================

    private String formatDuration(long millis) {

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {

            return String.format(
                    "%d dakika %d saniye",
                    minutes,
                    remainingSeconds
            );
        }

        return String.format(
                "%d saniye",
                remainingSeconds
        );
    }
}