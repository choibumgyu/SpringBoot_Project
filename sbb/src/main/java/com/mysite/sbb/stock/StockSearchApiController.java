package com.mysite.sbb.stock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@RestController
public class StockSearchApiController {

    private final StockMasterService stockMasterService;

    public StockSearchApiController(StockMasterService stockMasterService) {
        this.stockMasterService = stockMasterService;
    }

    // ✅ 프론트(stock.html)가 호출할 API
    // GET /api/stocks/search?keyword=삼성
    @GetMapping("/api/stocks/search")
    public List<StockSearchResult> search(
            @RequestParam("keyword") String keyword
    ) {
        return stockMasterService.search(keyword, 10);
    }
}
