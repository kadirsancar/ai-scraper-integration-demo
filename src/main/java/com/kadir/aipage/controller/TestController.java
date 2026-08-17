
package com.kadir.aipage.controller;

import com.kadir.aipage.mcp.ECommerceTool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final ECommerceTool eCommerceTool;

    public TestController(ECommerceTool eCommerceTool) {
        this.eCommerceTool = eCommerceTool;
    }

    @GetMapping("/price")
    public String testPrice(@RequestParam String url) {
        return eCommerceTool.getProductPrice(url);
    }
}