package com.kadir.aipage.mcp;

import com.kadir.aipage.entity.PriceHistory;
import com.kadir.aipage.entity.Product;
import com.kadir.aipage.repository.PriceHistoryRepository;
import com.kadir.aipage.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PriceHistoryTool {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistoryTool(ProductRepository productRepository, PriceHistoryRepository priceHistoryRepository) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public String getProductPriceHistory(String productNameKeyword) {
        if (productNameKeyword == null || productNameKeyword.isBlank()) {
            return "Geçersiz ürün adı anahtar kelimesi.";
        }

        // Ürün adında geçen kelimeye göre ürünü bulalım
        List<Product> products = productRepository.findByNameContainingIgnoreCase(productNameKeyword);
        if (products.isEmpty()) {
            return "Veritabanında '" + productNameKeyword + "' ile eşleşen kayıtlı bir ürün bulunamadı.";
        }

        StringBuilder sb = new StringBuilder();
        for (Product product : products) {
            sb.append("Ürün: ").append(product.getName())
                    .append(" | Platform: ").append(product.getPlatform()).append("\n");

            List<PriceHistory> histories = priceHistoryRepository.findByProductIdOrderByCheckedAtAsc(product.getId());
            if (histories.isEmpty()) {
                sb.append("-> Bu ürün için henüz fiyat geçmişi kaydı bulunmuyor.\n\n");
            } else {
                sb.append("Geçmiş Fiyat Listesi:\n");
                for (PriceHistory h : histories) {
                    sb.append(String.format("- Tarih: %s | Fiyat: %s TL | Orijinal Fiyat: %s | İndirim: %s\n",
                            h.getCheckedAt(),
                            h.getCurrentPrice(),
                            h.getOriginalPrice() != null ? h.getOriginalPrice() + " TL" : "Yok",
                            h.getDiscountInfo() != null ? h.getDiscountInfo() : "Yok"
                    ));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}