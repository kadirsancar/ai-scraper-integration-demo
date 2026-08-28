package com.kadir.aipage.service;

import com.kadir.aipage.entity.PriceHistory;
import com.kadir.aipage.entity.Product;
import com.kadir.aipage.repository.PriceHistoryRepository;
import com.kadir.aipage.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ProductTrackingService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public ProductTrackingService(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional
    public PriceHistory savePriceRecord(String productUrl, String platform, String productName,
                                        BigDecimal currentPrice, BigDecimal originalPrice, String discountInfo) {

        // 1. Ürün daha önce kaydedilmiş mi kontrol et, yoksa kaydet
        Product product = productRepository.findByUrl(productUrl)
                .orElseGet(() -> {
                    Product newProduct = new Product();
                    newProduct.setUrl(productUrl);
                    newProduct.setPlatform(platform);
                    newProduct.setName(productName != null ? productName : "Bilinmeyen Ürün");
                    return productRepository.save(newProduct);
                });

        // 2. Fiyat geçmişi tablosuna yeni kaydı ekle (Zaman serisi mantığı)
        PriceHistory history = new PriceHistory();
        history.setProduct(product);
        history.setCurrentPrice(currentPrice);
        history.setOriginalPrice(originalPrice);
        history.setDiscountInfo(discountInfo);
        history.setCheckedAt(LocalDateTime.now());

        return priceHistoryRepository.save(history);
    }
}