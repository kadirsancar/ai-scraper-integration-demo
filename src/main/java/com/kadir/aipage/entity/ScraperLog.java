package com.kadir.aipage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraper_logs")
public class ScraperLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String platform;

    @Column(columnDefinition = "TEXT")
    private String productUrl;

    private long totalDurationMs;

    @Column(columnDefinition = "TEXT")
    private String formattedDuration;

    private long driverInitDurationMs;
    private long pageLoadDurationMs;
    private long titleFindDurationMs;
    private long priceFindDurationMs;

    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }

    public String getFormattedDuration() { return formattedDuration; }
    public void setFormattedDuration(String formattedDuration) { this.formattedDuration = formattedDuration; }

    public long getDriverInitDurationMs() { return driverInitDurationMs; }
    public void setDriverInitDurationMs(long driverInitDurationMs) { this.driverInitDurationMs = driverInitDurationMs; }

    public long getPageLoadDurationMs() { return pageLoadDurationMs; }
    public void setPageLoadDurationMs(long pageLoadDurationMs) { this.pageLoadDurationMs = pageLoadDurationMs; }

    public long getTitleFindDurationMs() { return titleFindDurationMs; }
    public void setTitleFindDurationMs(long titleFindDurationMs) { this.titleFindDurationMs = titleFindDurationMs; }

    public long getPriceFindDurationMs() { return priceFindDurationMs; }
    public void setPriceFindDurationMs(long priceFindDurationMs) { this.priceFindDurationMs = priceFindDurationMs; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}