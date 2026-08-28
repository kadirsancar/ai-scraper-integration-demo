package com.kadir.aipage.mcp;

import com.kadir.aipage.dto.ProductPriceInfo;
import com.kadir.aipage.entity.ScraperLog;
import com.kadir.aipage.mcp.scraper.ECommerceScraper;
import com.kadir.aipage.repository.ScraperLogRepository;
import com.kadir.aipage.service.ProductTrackingService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ECommerceTool {

    private final List<ECommerceScraper> scrapers;
    private final ProductTrackingService productTrackingService;
    private final ScraperLogRepository scraperLogRepository;

    public ECommerceTool(List<ECommerceScraper> scrapers,
                         ProductTrackingService productTrackingService,
                         ScraperLogRepository scraperLogRepository) {
        this.scrapers = scrapers;
        this.productTrackingService = productTrackingService;
        this.scraperLogRepository = scraperLogRepository;
    }

    public String getProductPrice(String productUrl) {
        if (productUrl == null || productUrl.isBlank()) {
            return "Geçersiz ürün URL'si.";
        }

        for (ECommerceScraper scraper : scrapers) {
            if (scraper.supports(productUrl)) {
                String platformName = scraper.getClass().getSimpleName();
                System.out.println("Uygun scraper bulundu: " + platformName);

                long startTime = System.currentTimeMillis();
                boolean success = false;
                String errorMessage = null;
                ProductPriceInfo info = null;

                try {
                    info = scraper.getProductDetails(productUrl);
                    if (info != null && info.getCurrentPrice() != null) {
                        success = true;
                    } else {
                        errorMessage = info != null ? info.getProductName() : "Fiyat okunamadı";
                    }
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    success = false;
                }

                long endTime = System.currentTimeMillis();
                long durationMs = endTime - startTime;
                String formattedTime = formatDuration(durationMs);

                System.out.println("⏱️ [" + platformName + "] Geçen Süre: " + formattedTime);

                // Logu Veritabanına Kaydet
                ScraperLog log = new ScraperLog();
                log.setPlatform(platformName);
                log.setProductUrl(productUrl);
                log.setTotalDurationMs(durationMs);
                log.setFormattedDuration(formattedTime);

                // Adım Sürelerini Veritabanına Aktarıyoruz
                if (info != null) {
                    log.setDriverInitDurationMs(info.getDriverInitMs());
                    log.setPageLoadDurationMs(info.getPageLoadMs());
                    log.setTitleFindDurationMs(info.getTitleFindMs());
                    log.setPriceFindDurationMs(info.getPriceFindMs());
                }

                log.setSuccess(success);
                log.setErrorMessage(errorMessage);
                log.setCreatedAt(LocalDateTime.now());
                scraperLogRepository.save(log);

                if (success) {
                    productTrackingService.savePriceRecord(
                            info.getProductUrl(),
                            info.getPlatform(),
                            info.getProductName(),
                            info.getCurrentPrice(),
                            info.getOriginalPrice(),
                            info.getDiscountInfo()
                    );

                    return String.format("Ürün: %s | Fiyat: %s TL | Platform: %s (Süre: %s, Veritabanına kaydedildi)",
                            info.getProductName(), info.getCurrentPrice(), info.getPlatform(), formattedTime);
                } else {
                    return "Ürün bilgileri çekildi ancak fiyat okunamadı veya hata oluştu: " + errorMessage;
                }
            }
        }
        return "Bu URL adresini destekleyen herhangi bir e-ticaret scraper stratejisi bulunamadı.";
    }

    public String analyzeProductByName(String productName) {
        if (productName == null || productName.isBlank()) {
            return "Geçersiz ürün adı.";
        }

        System.out.println("🤖 AI Asistanı ürün adı ile çoklu platform araması başlattı: " + productName);

        StringBuilder analysisResult = new StringBuilder();
        analysisResult.append(String.format("'%s' ürünü için e-ticaret sitelerindeki arama ve fiyat tarama sonuçları:\n", productName));

        boolean foundAny = false;

        for (ECommerceScraper scraper : scrapers) {
            try {
                String platformName = scraper.getClass().getSimpleName();
                System.out.println(platformName + " üzerinden arama yapılıyor...");

                // Her bir scraper üzerinden ürün adıyla arama yapıp fiyat bilgisini alıyoruz
                ProductPriceInfo info = scraper.searchAndGetProduct(productName);

                if (info != null && info.getCurrentPrice() != null) {
                    foundAny = true;
                    analysisResult.append(String.format("- Platform: %s | Ürün: %s | Fiyat: %s TL | İndirim: %s\n",
                            info.getPlatform(),
                            info.getProductName(),
                            info.getCurrentPrice(),
                            info.getDiscountInfo() != null ? info.getDiscountInfo() : "Yok"
                    ));

                    // Fiyat kaydını veritabanına işleyelim
                    productTrackingService.savePriceRecord(
                            info.getProductUrl(),
                            info.getPlatform(),
                            info.getProductName(),
                            info.getCurrentPrice(),
                            info.getOriginalPrice(),
                            info.getDiscountInfo()
                    );
                } else {
                    analysisResult.append(String.format("- Platform: %s | Bu ürüne ait fiyat bulunamadı.\n", platformName));
                }
            } catch (Exception e) {
                analysisResult.append(String.format("- Platform tarama hatası: %s\n", e.getMessage()));
            }
        }

        if (!foundAny) {
            return "Belirtilen ürün adı için hiçbir platformda geçerli fiyat bilgisine ulaşılamadı.";
        }

        return analysisResult.toString();
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%d dakika %d saniye", minutes, remainingSeconds);
        } else {
            return String.format("%d saniye", remainingSeconds);
        }
    }
}