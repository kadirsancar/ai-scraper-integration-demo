package com.kadir.aipage.mcp.scraper;


import com.kadir.aipage.dto.ProductPriceInfo;

public interface ECommerceScraper {
    boolean supports(String url);

    ProductPriceInfo getProductDetails(String productUrl);

    // Ürün adına göre arama yapıp sonuç döndürme (Yeni)
    ProductPriceInfo searchAndGetProduct(String productName);
}