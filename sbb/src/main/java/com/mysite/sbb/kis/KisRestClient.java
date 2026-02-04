package com.mysite.sbb.kis;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
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

    /**
     * 전일 종가/등락률 등 "한번 가져오면 되는" 정보 조회
     * (국내주식 현재가/기준가 조회 API 예시)
     */
    public StockSummary getStockSummary(String code) {

        // ✅ KIS REST 엔드포인트 (예시)
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";

        // ✅ 헤더 구성 (아래 값들은 너의 KisApprovalService가 제공하는 값에 맞춰 수정)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // TODO: 너 서비스에 access token 발급 메서드가 있으면 사용
        // headers.set("authorization", "Bearer " + approvalService.getAccessToken());

        // TODO: appkey/appsecret을 properties로 관리하고 있으면 주입받거나 서비스에서 제공
        // headers.set("appkey", approvalService.getAppKey());
        // headers.set("appsecret", approvalService.getAppSecret());

        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("authorization", "Bearer " + approvalService.getAccessToken());
        headers.set("tr_id", "FHKST01010100");


        // ✅ Query Param
        String finalUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("fid_cond_mrkt_div_code", "J")
                .queryParam("fid_input_iscd", code)
                .toUriString();

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        log.info("[KIS-REST] url={}", finalUrl);
        log.info("[KIS-REST] headers: authorization={}, appkey={}, appsecret={}, tr_id={}",
                headers.getFirst("authorization") != null,
                headers.getFirst("appkey") != null,
                headers.getFirst("appsecret") != null,
                headers.getFirst("tr_id"));

        ResponseEntity<String> response =
                restTemplate.exchange(finalUrl, HttpMethod.GET, entity, String.class);

        try {
            JsonNode root = om.readTree(response.getBody());
            JsonNode out = root.path("output");

            // ✅ 필드명은 실제 KIS 응답에 맞춰 조정 필요
            String current = out.path("stck_prpr").asText("");   // 현재가
            String prevClose = out.path("stck_sdpr").asText(""); // 전일 종가
            //String diff = out.path("prdy_vrss").asText("");      // 전일 대비
            //String rate = out.path("prdy_ctrt").asText("");      // 등락률(%)

            return new StockSummary(prevClose,current);

        } catch (Exception e) {
            throw new RuntimeException("KIS summary parse failed", e);
        }
    }

    // DTO를 inner class로 두면 파일 하나로 컴파일 쉬움
    public record StockSummary(
            String prevClose, // 전일 종가
            String current    // 현재가(REST)
    ) {}
}
