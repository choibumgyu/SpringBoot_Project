package com.mysite.sbb.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMasterBootstrap implements ApplicationRunner {

    private final StockMasterService stockMasterService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long cnt = stockMasterService.count();
        if (cnt > 0) {
            log.info("[BOOT] stock_master already loaded. cnt={}", cnt);
            return;
        }

        // ✅ resources/stocks/kospi_code.mst
        var resource = new ClassPathResource("stocks/kospi_code.mst");

        // ✅ KRX 계열 파일은 CP949/MS949가 많음
        Charset cs = Charset.forName("CP949");

        int inserted = stockMasterService.loadFromMst(resource, StandardCharsets.UTF_8);
        log.info("[BOOT] stock_master loaded. inserted={}", inserted);
    }
}
