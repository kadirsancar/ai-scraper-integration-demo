package com.kadir.aipage.dto;

public class ScraperExecutionResult {
    private ProductPriceInfo productPriceInfo;
    private long driverInitMs;
    private long pageLoadMs;
    private long titleFindMs;
    private long priceFindMs;

    // Getter ve Setter'lar
    public ProductPriceInfo getProductPriceInfo() { return productPriceInfo; }
    public void setProductPriceInfo(ProductPriceInfo productPriceInfo) { this.productPriceInfo = productPriceInfo; }
    public long getDriverInitMs() { return driverInitMs; }
    public void setDriverInitMs(long driverInitMs) { this.driverInitMs = driverInitMs; }
    public long getPageLoadMs() { return pageLoadMs; }
    public void setPageLoadMs(long pageLoadMs) { this.pageLoadMs = pageLoadMs; }
    public long getTitleFindMs() { return titleFindMs; }
    public void setTitleFindMs(long titleFindMs) { this.titleFindMs = titleFindMs; }
    public long getPriceFindMs() { return priceFindMs; }
    public void setPriceFindMs(long priceFindMs) { this.priceFindMs = priceFindMs; }
}