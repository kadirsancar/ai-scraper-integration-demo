package com.kadir.aipage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPriceInfo {
    private String productName;
    private BigDecimal currentPrice;
    private BigDecimal originalPrice;
    private String discountInfo;
    private String platform;
    private String productUrl;

    // Adım Adım Süreler (Ms)
    private long driverInitMs;
    private long pageLoadMs;
    private long titleFindMs;
    private long priceFindMs;
}