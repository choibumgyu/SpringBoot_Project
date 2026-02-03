package com.mysite.sbb.stock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mysite.sbb.kis.KisRestClient;
import com.mysite.sbb.kis.KisRestClient.StockSummary;

@RestController
@RequestMapping("/api/stocks")
public class StockApiController {

    private final KisRestClient kisRestClient;

    public StockApiController(KisRestClient kisRestClient) {
        this.kisRestClient = kisRestClient;
    }

    // 👉 전일 종가 / 등락률 / 기준가 등 "1회 조회" 정보
    @GetMapping("/{code}/summary")
    public StockSummary getSummary(@PathVariable("code") String code) {
        return kisRestClient.getStockSummary(code);
    }

}
