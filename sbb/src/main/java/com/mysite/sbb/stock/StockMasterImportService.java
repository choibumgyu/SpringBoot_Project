package com.mysite.sbb.stock;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@Service
public class StockMasterImportService {

    private final StockMasterService stockMasterService;

    public StockMasterImportService(StockMasterService stockMasterService) {
        this.stockMasterService = stockMasterService;
    }

    /**
     * kospi_code.mst (고정폭 + EUC-KR) -> stock_master 업서트
     * 예상 포맷(대표):
     * 0~5   : code (6)
     * 6~8   : space
     * 9~20  : isin (12)
     * 21~50 : name (30)
     */
    @Transactional
    public ImportResult importKospiFromClasspath() {
        int total = 0;
        int success = 0;
        int skipped = 0;

        try {
            ClassPathResource resource = new ClassPathResource("stocks/kospi_code.mst");
            Charset eucKr = Charset.forName("EUC-KR");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), eucKr))) {
                String line;
                while ((line = br.readLine()) != null) {
                    total++;

                    // 너무 짧은 라인은 스킵
                    if (line.length() < 30) {
                        skipped++;
                        continue;
                    }

                    String code = safeSub(line, 0, 6).trim();      // 005930
                    String isin = safeSub(line, 9, 21).trim();     // KR7005930003 등
                    String name = safeSub(line, 21, 51).trim();    // 삼성전자

                    if (code.isEmpty() || name.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    stockMasterService.upsert(code, name, isin, "KOSPI");
                    success++;
                }
            }

            return new ImportResult(total, success, skipped, null);

        } catch (Exception e) {
            return new ImportResult(total, success, skipped, e.getMessage());
        }
    }

    private String safeSub(String s, int start, int endExclusive) {
        if (s == null) return "";
        if (start >= s.length()) return "";
        int end = Math.min(endExclusive, s.length());
        return s.substring(start, end);
    }

    public record ImportResult(int total, int success, int skipped, String error) {}
}
