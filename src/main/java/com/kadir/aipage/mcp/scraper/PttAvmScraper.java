package com.kadir.aipage.mcp.scraper;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

@Component
public class PttAvmScraper implements ECommerceScraper{

    @Override
    public boolean supports(String url){
        return url != null  && url.contains("pttavm.com");
    }

    @Override
    public String getPrice(String producUrl) {
        WebDriver driver = null;

        try {
            System.out.println("PttAvmScraper devrede, URL: " + producUrl);

            ChromeOptions options = new ChromeOptions();

            options.addArguments("--headless=new");
            options.addArguments("--dısable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-new-shb-usage");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

            driver = new ChromeDriver(options);
            driver.get(producUrl);

            Thread.sleep(4000);

            JavascriptExecutor js = (JavascriptExecutor) driver;

            String script = "try {" +
                    "  let data = window.__staticRouterHydrationData;" +
                    "  if (!data) return 'JSON verisi bulunamadı';" +
                    "  let jsonStr = JSON.stringify(data);" +
                    "  let match = jsonStr.match(/\\\"discountedPriceText\\\":\\\"([^\"]+)\\\"/);" +
                    "  return match ? match[1] : 'Fiyat bilgisi eşleşmedi';" +
                    "} catch(e) {" +
                    "  return 'Hata: ' + e.message;" +
                    "}";

            String priceText = (String)  js.executeScript(script);

            if (priceText != null && !priceText.isEmpty()) {
                return "Ürün güncel fiyatı :" + priceText;
            } else   {
                return "Sayfa yüklendi ama PttAvm JSON verisinden fiyat okunamadı.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "PttAvm otamasyon hatası:" + e.getMessage();
        } finally {
            if (driver!=null){
                driver.quit();
            }
        }
    }
}
