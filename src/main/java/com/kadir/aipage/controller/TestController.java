package com.kadir.aipage.controller;

import com.kadir.aipage.dto.ProductPriceInfo;
import com.kadir.aipage.mcp.scraper.HepsiBuradaScraper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-search")
public class TestController {

    private final HepsiBuradaScraper hepsiBuradaScraper;

    public TestController(HepsiBuradaScraper hepsiBuradaScraper) {
        this.hepsiBuradaScraper = hepsiBuradaScraper;
    }

    @GetMapping("/price")
    public ProductPriceInfo testPrice(
            @RequestParam String product) {

        return hepsiBuradaScraper.searchAndGetProduct(product);
    }
}