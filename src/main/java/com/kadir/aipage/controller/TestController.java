 package com.kadir.aipage.controller;

import com.kadir.aipage.dto.ProductPriceInfo;
import com.kadir.aipage.mcp.scraper.TrendyolScraper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-search")
public class TestController {

    private final TrendyolScraper trendyolScraper;

    public TestController(TrendyolScraper trendyolScraper) {
        this.trendyolScraper = trendyolScraper;
    }

    @GetMapping("/trendyol")
    public ProductPriceInfo testTrendyol(
            @RequestParam String product) {

        return trendyolScraper.searchAndGetProduct(product);
    }
}

