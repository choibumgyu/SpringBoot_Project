package com.mysite.sbb.ai;

import com.mysite.sbb.ai.dto.BriefingRequestSubmitResponse;
import com.mysite.sbb.ai.service.BriefingRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StockBriefingController {

    private final StockBriefingService service;
    private final BriefingRequestService briefingRequestService;

    @PostMapping("/api/stock-briefing")
    public Map<String, Object> briefing(@RequestBody StockBriefingRequest req) {
        log.info("briefing req = {}", req);
        return service.generate(req);
    }

    @PostMapping("/api/stock-briefing/request")
    public BriefingRequestSubmitResponse submitBriefingRequest(@RequestBody StockBriefingRequest req) {
        log.info("briefing request accepted. stockName={}", req.stockName());
        return briefingRequestService.submit(req);
    }
}