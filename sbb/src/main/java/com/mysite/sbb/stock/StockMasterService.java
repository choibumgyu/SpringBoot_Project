package com.mysite.sbb.stock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockMasterService {

    private final StockMasterRepository repo;
    
    public long count() {
        return repo.count();
    }
    /**
     * ⚠️ MST 포맷(고정폭)이 파일마다 다를 수 있음.
     * 아래는 "우선 적재가 되게" 만드는 임시 파서(예시)라서,
     * 네 mst 실제 첫 몇 줄을 보고 정확한 파싱으로 바꾸는 게 필요함.
     */
    
    private static final Pattern ISIN_PATTERN = Pattern.compile("KR[0-9A-Z]{10}");
    private static final Pattern NAME_END_PATTERN = Pattern.compile("\\s{2,}[A-Z]{2}");

    @Transactional
    public int loadFromMst(Resource resource, Charset charset) throws Exception {
        // ✅ 여기서 charset을 강제해버리는 게 가장 안전함 (리눅스 UTF-8 기본값 이슈 차단)
        //Charset charset = Charset.forName("MS949"); // 또는 "CP949"

        int inserted = 0;
        int skipped = 0;

        try (InputStream is = resource.getInputStream();
        	BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                // ❌ 고정폭 파일에 strip() 쓰면 위험 (공백이 의미 있는 필드일 수 있음)
                // line = line.strip();

                if (line.isEmpty()) continue;

                try {
                    // 1) ISIN을 위치가 아니라 "패턴"으로 찾기
                    Matcher isinMatcher = ISIN_PATTERN.matcher(line);
                    if (!isinMatcher.find()) {
                        skipped++;
                        continue;
                    }
                    String isin = isinMatcher.group(); // 예: KR7005930003

                    // 2) code 추출: 일반 주식은 KR7 + 6자리(code)
                    String code;
                    if (isin.charAt(2) == '7') {
                        code = isin.substring(3, 9);
                    } else {
                        // ETF/ETN 등은 파일마다 다를 수 있어 일단 라인 앞 6자리 우선 사용
                        // (필요하면 여기 정책을 더 정교화)
                        if (line.length() >= 6 && line.substring(0, 6).chars().allMatch(Character::isDigit)) {
                            code = line.substring(0, 6);
                        } else {
                            code = isin.substring(2, 8);
                        }
                    }

                    if (code.length() != 6 || !code.chars().allMatch(Character::isDigit)) {
                        log.warn("[MST][SKIP] invalid code={} isin={} line={}", code, isin, preview(line));
                        skipped++;
                        continue;
                    }

                    // 3) name 추출: ISIN 끝난 지점부터 이름이 시작
                    int nameStart = isinMatcher.end();
                    String tail = line.substring(nameStart);

                    Matcher m = NAME_END_PATTERN.matcher(tail);
                    String name = m.find() ? tail.substring(0, m.start()).trim() : tail.trim();

                    // (안전장치) name이 너무 길면 자르기
                    if (name.length() > 200) name = name.substring(0, 200);

                    if (name.isBlank()) {
                        log.warn("[MST][SKIP] blank name. isin={} code={} line={}", isin, code, preview(line));
                        skipped++;
                        continue;
                    }

                    // 4) 중복 스킵
                    if (repo.existsById(code)) {
                        skipped++;
                        continue;
                    }

                    StockMaster sm = new StockMaster();
                    sm.setCode(code);
                    sm.setIsin(isin);
                    sm.setName(name);
                    sm.setMarket("KOSPI");
                    sm.setIsActive(true);
                    sm.setUpdatedAt(LocalDateTime.now());

                    repo.save(sm);
                    inserted++;

                } catch (Exception rowEx) {
                    log.warn("[MST] skip bad row. msg={}, line={}", rowEx.getMessage(), preview(line));
                    skipped++;
                }
            }
        }

        log.info("[MST] load done. inserted={}, skipped={}", inserted, skipped);
        return inserted;
    }

    private static String safeSub(String s, int start, int end) {
        int e = Math.min(end, s.length());
        int st = Math.min(start, s.length());
        if (st >= e) return "";
        return s.substring(st, e);
    }

    private static String preview(String s) {
        if (s == null) return "";
        return s.length() <= 80 ? s : s.substring(0, 80) + "...";
    }
    @Transactional(readOnly = true)
    public List<StockSearchResult> search(String keyword, int limit) {
        if (keyword == null) keyword = "";
        String k = keyword.trim();
        if (k.isEmpty()) return List.of();

        List<StockMaster> found = repo.searchByName(k);
        return found.stream()
                .limit(Math.max(1, Math.min(limit, 50))) // 안전장치
                .map(s -> new StockSearchResult(s.getCode(), s.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StockMaster getOrNull(String code) {
        if (code == null) return null;
        return repo.findById(code).orElse(null);
    }

    @Transactional
    public StockMaster upsert(String code, String name, String isin, String market) {
        StockMaster existing = repo.findById(code).orElse(null);
        if (existing == null) {
            StockMaster created = new StockMaster(code, name, isin, market);
            return repo.save(created);
        }
        // 변경 사항 반영(업서트)
        existing.setName(name);
        existing.setIsin(isin);
        existing.setMarket(market);
        existing.setIsActive(true);
        return repo.save(existing);
    }
}
