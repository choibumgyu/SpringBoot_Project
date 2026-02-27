package com.mysite.sbb.kis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KisRestClient {

    @Value("${kis.rest.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    private final KisApprovalService approvalService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    public KisRestClient(KisApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    public StockSummary getStockSummary(String code) {

        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("authorization", "Bearer " + approvalService.getAccessToken());
        headers.set("tr_id", "FHKST01010100");

        String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("fid_cond_mrkt_div_code", "J")
                .queryParam("fid_input_iscd", code)
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.info("[KIS-REST] GET {}", finalUrl);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(finalUrl, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("KIS REST non-2xx: " + response.getStatusCode());
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new RuntimeException("KIS REST empty body");
            }

            JsonNode root = om.readTree(body);

            // (선택) KIS 응답에 에러 구조가 있을 수도 있어서 메시지 후보를 뽑아둠
            // - 실제 키는 KIS 응답 스펙에 따라 다를 수 있음
            JsonNode out = root.path("output");
            if (out.isMissingNode() || out.isNull() || out.size() == 0) {
                String msg = root.path("msg1").asText(""); // 종종 이런 형태가 있을 때가 있음
                throw new RuntimeException("KIS REST missing output. msg=" + msg);
            }

            String current = out.path("stck_prpr").asText("");   // 현재가
            String prevClose = out.path("stck_sdpr").asText(""); // 전일 종가

            return new StockSummary(prevClose, current);

        } catch (RestClientResponseException e) {
            // 4xx/5xx가 났을 때 응답 바디를 같이 로그로 남겨주면 디버깅이 쉬움
            log.error("[KIS-REST] HTTP error status={} body={}", e.getRawStatusCode(), safeBody(e.getResponseBodyAsString()));
            throw new RuntimeException("KIS REST HTTP error: " + e.getRawStatusCode(), e);

        } catch (Exception e) {
            log.error("[KIS-REST] parse/unknown error", e);
            throw new RuntimeException("KIS summary parse failed", e);
        }
    }

    private String safeBody(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    public record StockSummary(
            String prevClose,
            String current
    ) {}
}
