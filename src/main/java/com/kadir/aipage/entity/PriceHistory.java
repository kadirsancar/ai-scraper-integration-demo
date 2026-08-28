package com.kadir.aipage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_histories")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private BigDecimal currentPrice;    // Güncel Fiyat
    private BigDecimal originalPrice;   // Eski / Çizili Fiyat (Varsa)
    private String discountInfo;        // İndirim oranı veya kazanç bilgisi (Örn: %15)

    private LocalDateTime checkedAt = LocalDateTime.now(); // Fiyatın çekildiği tarih/saat
}