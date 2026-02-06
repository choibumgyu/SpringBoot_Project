package com.mysite.sbb.stock;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.mysite.sbb.kis.KisRestClient;
import com.mysite.sbb.kis.KisRestClient.StockSummary;

@RestController
@RequestMapping("/api/stocks")
public class StockApiController {

    private final KisRestClient kisRestClient;
    private final StockMasterService stockMasterService; // ✅ 추가

    public StockApiController(KisRestClient kisRestClient, StockMasterService stockMasterService) {
        this.kisRestClient = kisRestClient;
        this.stockMasterService = stockMasterService;
    }

    /**
     * ✅ 종목명 검색 (자동완성)
     * GET /api/stocks/search?keyword=삼성
     */
    @GetMapping("/search")
    public List<StockSearchResult> search(@RequestParam("keyword") String keyword) {
        return stockMasterService.search(keyword, 10);
    }

    /**
     * 👉 전일 종가 / 등락률 / 기준가 등 "1회 조회" 정보
     */
    @GetMapping("/{code}/summary")
    public StockSummary getSummary(@PathVariable("code") String code) {

        // ✅ (추천) 종목코드 유효성 검증: DB에 존재하지 않으면 404
        // - 이걸 빼도 동작은 하는데, 운영/면접 관점에서 있으면 좋음
        StockMaster sm = stockMasterService.getOrNull(code);
        if (sm == null || Boolean.FALSE.equals(sm.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown stock code: " + code);
        }

        return kisRestClient.getStockSummary(code);
    }
}
