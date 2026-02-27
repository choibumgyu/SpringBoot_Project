package com.mysite.sbb.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class StockBriefingService {

  private final GeminiClient geminiClient;
  private final ObjectMapper om;

  
  @Cacheable(
		    cacheNames = "stockBriefing",
		    key = "'stock:' + #p0.stockName().trim().toUpperCase()",
		    unless = "#result == null || #result.isEmpty()"
		)
  public Map<String, Object> generate(StockBriefingRequest req) {
	  
	  StockBriefingRequest limitedReq = new StockBriefingRequest(
		        req.stockName(),
		        req.currentPrice(),
		        req.prevClose(),
		        req.news().stream().limit(3).toList() 
		    );
    String prompt = buildPrompt(limitedReq);

    String raw = geminiClient.generateJson(
        "gemini-2.5-flash",
        prompt,
        0.2,
        4096
    );

    String json = extractJsonObject(raw);

    try {
      return om.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException("Briefing JSON parse failed: " + raw, e);
    }
  }

  private String buildPrompt(StockBriefingRequest req) {
    StringBuilder newsBlock = new StringBuilder();
    int idx = 1;
    for (var n : req.news()) {
    	String desc = nullToEmpty(n.description());
        if (desc.length() > 200) desc = desc.substring(0, 200) + "...";
      newsBlock.append(idx++).append(") 제목: ").append(nullToEmpty(n.title())).append("\n")
          .append("   요약: ").append(nullToEmpty(n.description())).append("\n")
          .append("   링크: ").append(nullToEmpty(n.link())).append("\n\n");
    }

    return """
너는 금융 애널리스트 역할을 수행하는 주식 브리핑 AI다.

[목표]
제공된 가격 정보와 뉴스 요약을 기반으로 해당 종목의 현재 상황을 객관적으로 브리핑하라.

[가격 해석 규칙]
1. 현재가 > 전일 종가이면, 상승 배경을 중심으로 분석하라. 이 경우 summary_3lines의 첫 줄은 상승 원인을 설명해야 한다.
2. 현재가 < 전일 종가이면, 하락 배경을 중심으로 분석하라. 이 경우 summary_3lines의 첫 줄은 하락 원인을 설명해야 한다.
3. 가격 변화의 방향성은 반드시 뉴스 내용과 연결지어 설명하라.
4. 가격 정보만으로 독립적인 해석을 하지 말라.

[중요 규칙]
5. 반드시 아래 제공된 정보만을 근거로 작성하라.
6. 뉴스에 포함되지 않은 내용을 추측하거나 생성하지 말라.
7. 과장 표현, 투자 권유 문구를 사용하지 말라.
8. 제공된 뉴스에 긍정 요인이 없으면 bull_points는 빈 배열([])로 반환하라.
9. 제공된 뉴스에 부정 요인이 없으면 bear_points는 빈 배열([])로 반환하라.
10. 출력은 반드시 JSON 객체 하나만 반환하라.
11. JSON 외의 텍스트, 코드블록, 설명을 절대 포함하지 말라.
12. 모든 bull_points, bear_points, watch_points 객체는 "point"와 "evidence_links" 필드를 반드시 포함해야 한다.
13. sources의 각 항목에는 반드시 "title"과 "link"를 포함해야 한다.
14. 위 JSON 스키마의 필드를 누락하지 말라.
15. 마크다운 기호나 json같은 표시를 절대 포함하지 마.오직 순수한 json 문자열만 응답해줘. 
16. 모든 bull_points, bear_points, watch_points의 evidence_links는 반드시 딱 1개만 포함하라.
17. sources 항목 또한 가장 관련 있는 뉴스 딱 2개만 포함하라.
18. 각 포인트(point) 설명은 한 문장으로 간결하게 작성하라.

[입력 데이터]
종목명: %s
현재가: %s
전일 종가: %s

관련 뉴스:
%s

[출력 JSON 스키마]
{
  "summary_3lines": ["", "", ""],
  "bull_points": [{"point":"", "evidence_links":[""]}],
  "bear_points": [{"point":"", "evidence_links":[""]}],
  "watch_points": [{"point":"", "evidence_links":[""]}],
  "sources": [{"title":"", "link":""}],
  "confidence": 0.0
}
""".formatted(req.stockName(), req.currentPrice(), req.prevClose(), newsBlock.toString());
  }

  private String extractJsonObject(String s) {
	    if (s == null || s.isBlank()) return "{}";
	    
	    // 1. 앞뒤 공백 및 마크다운 코드 블록 표시 제거
	    s = s.trim();
	    if (s.startsWith("```")) {
	        s = s.replaceAll("^```(?:json)?", "").replaceAll("```$", "").trim();
	    }
	    
	    // 2. 가장 처음 나타나는 { 와 가장 마지막에 나타나는 } 를 추출
	    int start = s.indexOf('{');
	    int end = s.lastIndexOf('}');
	    
	    if (start >= 0 && end > start) {
	        return s.substring(start, end + 1);
	    }
	    
	    return s; // 찾지 못한 경우 원본 반환 (ObjectMapper에서 에러를 던지도록)
	}
  private String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}