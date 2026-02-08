package com.mysite.sbb.news;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private final RestClient restClient;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    // "주가|증시|주식|실적" 필터
    private static final Pattern STOCK_CONTEXT =
            Pattern.compile("(주가|증시|주식|실적|증권|코스피|시장|매수|매도|등락|상승|하락|이익|공시|매출|장중|거래)");

    // 네이버 title/description에 섞여 오는 <b>...</b> 제거용
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    public List<NaverNewsResponse.NaverNewsItem> searchNews(String keyword, int display) {
        int safeDisplay = Math.max(1, Math.min(display, 100));

        // ✅ query는 넓게 가져오고, 필터로 품질을 보장하는 방식이 안정적
        String refined = "\"" + keyword.trim() + "\"";

        NaverNewsResponse res = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("openapi.naver.com")
                        .path("/v1/search/news.json")
                        .queryParam("query", refined)
                        .queryParam("display", safeDisplay)
                        .queryParam("start", 1)
                        .queryParam("sort", "date")
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .body(NaverNewsResponse.class);

        List<NaverNewsResponse.NaverNewsItem> items =
                (res == null || res.items() == null) ? List.of() : res.items();

        return filterItems(items, keyword);
    }

    private List<NaverNewsResponse.NaverNewsItem> filterItems(
            List<NaverNewsResponse.NaverNewsItem> items, String keyword) {

        String kw = normalize(keyword);

        return items.stream()
                .filter(it -> {
                    String title = normalize(it.title());
                    String content = normalize((it.title() == null ? "" : it.title())
                            + " " + (it.description() == null ? "" : it.description()));

                    // 조건 1) 제목에 종목명 포함
                    boolean titleHasKeyword = title.contains(kw);

                    // 조건 2) 내용(제목+요약)에 주식 컨텍스트 포함
                    boolean hasContext = STOCK_CONTEXT.matcher(content).find();

                    return titleHasKeyword && hasContext;
                })
                .toList();
    }

    private String normalize(String s) {
        if (s == null) return "";
        // HTML 태그 제거 (<b> 등)
        String noHtml = HTML_TAG.matcher(s).replaceAll("");
        // 소문자/공백 정리
        return noHtml.toLowerCase(Locale.ROOT).trim();
    }
}
