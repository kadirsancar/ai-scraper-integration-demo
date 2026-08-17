package com.kadir.aipage.mcp;

import com.kadir.aipage.mcp.scraper.ECommerceScraper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ECommerceTool {

    private final List<ECommerceScraper> scrapers;

    public ECommerceTool(List<ECommerceScraper> scrapers){
        this.scrapers=scrapers;
    }

    @Tool(description = "E-ticaret sitelerinden (PttAVM, Hepsiburada vb.) ürün URL'sini kullanarak dinamik olarak güncel fiyat bilgisini çeker.")
    public String getProductPrice(String productUrl){
        if (productUrl == null || productUrl.isBlank()) {
            return "Geçersiz ürün URL'si.";
        }


        for (ECommerceScraper scraper : scrapers){
            if (scraper.supports(productUrl)) {
                System.out.println("Uygun scraper bulundu:" + scraper.getClass().getSimpleName());
                return scraper.getPrice(productUrl);
            }
        }
        return "Bu URL adresini destekleyen herhangi bir e-ticaret scraper stratejisini bulunamadı.";
    }

}