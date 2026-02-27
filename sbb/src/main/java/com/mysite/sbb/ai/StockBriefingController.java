package com.mysite.sbb.ai;



import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StockBriefingController {

  private final StockBriefingService service;

  @PostMapping("/api/stock-briefing")
  public Map<String, Object> briefing(@RequestBody StockBriefingRequest req) {
	 log.info("briefing req = {}", req);
    return service.generate(req);
  }
}