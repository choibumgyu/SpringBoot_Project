package com.mysite.sbb.kis;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KisApprovalService {

    private final WebClient webClient;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.approval-base-url}")
    private String approvalBaseUrl;

    // ===== WS approval_key 캐시 =====
    private final AtomicReference<String> cachedApprovalKey = new AtomicReference<>();
    private volatile Instant approvalExpiresAt = Instant.EPOCH;

    // ===== REST access_token 캐시 =====
    private final AtomicReference<String> cachedAccessToken = new AtomicReference<>();
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public KisApprovalService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // ✅ WebSocket용 approval_key
    public synchronized String getApprovalKey() {
        if (cachedApprovalKey.get() != null && Instant.now().isBefore(approvalExpiresAt)) {
            return cachedApprovalKey.get();
        }

        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "secretkey", appSecret
        );

        var res = webClient.post()
                .uri(approvalBaseUrl + "/oauth2/Approval")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String approvalKey = (String) res.get("approval_key");
        cachedApprovalKey.set(approvalKey);

        approvalExpiresAt = Instant.now().plusSeconds(23 * 60 * 60);
        return approvalKey;
    }

    // ✅ REST용 access_token
    public synchronized String getAccessToken() {
        if (cachedAccessToken.get() != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken.get();
        }

        // 일반적으로 tokenP 엔드포인트 사용
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        var res = webClient.post()
        	    .uri(approvalBaseUrl + "/oauth2/tokenP")
        	    .contentType(MediaType.APPLICATION_JSON)
        	    .bodyValue(body)
        	    .retrieve()
        	    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
        	        clientResponse -> clientResponse.bodyToMono(String.class)
        	            .flatMap(errBody -> {
        	                log.error("[KIS][TOKEN] status={}, body={}", clientResponse.statusCode(), errBody);
        	                return reactor.core.publisher.Mono.error(new RuntimeException("KIS token error"));
        	            })
        	    )
        	    .bodyToMono(Map.class)
        	    .block();

        String token = (String) res.get("access_token");
        cachedAccessToken.set(token);

        // expires_in(초)이 내려오는 경우가 많음 → 있으면 그걸로, 없으면 보수적으로 50분
        Object expiresInObj = res.get("expires_in");
        long expiresIn = 50 * 60;
        if (expiresInObj != null) {
            try {
                expiresIn = Long.parseLong(expiresInObj.toString());
            } catch (Exception ignore) {}
        }

        // 만료 1분 전 갱신
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 60));
        return token;
    }

    // ✅ KisRestClient에서 헤더 세팅할 때 필요하면 getter로 제공
    public String getAppKey() { return appKey; }
    public String getAppSecret() { return appSecret; }
}
