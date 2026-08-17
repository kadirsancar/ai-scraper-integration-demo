package com.kadir.aipage.mcp.scraper;

public interface ECommerceScraper {
    boolean supports(String url);

    String getPrice(String url);
}
