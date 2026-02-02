package com.mysite.sbb.kis;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KisApprovalService {

    private final WebClient webClient;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.approval-base-url}")
    private String approvalBaseUrl;

    private final AtomicReference<String> cachedKey = new AtomicReference<>();
    private volatile Instant expiresAt = Instant.EPOCH;

    public KisApprovalService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public synchronized String getApprovalKey() {
        if (cachedKey.get() != null && Instant.now().isBefore(expiresAt)) {
            return cachedKey.get();
        }

        // POST {baseUrl}/oauth2/Approval
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
        cachedKey.set(approvalKey);

        // 접속키는 통상 24시간 사용(문서/가이드에서 자주 언급됨). :contentReference[oaicite:6]{index=6}
        expiresAt = Instant.now().plusSeconds(23 * 60 * 60); // 넉넉히 23시간 캐싱
        return approvalKey;
    }
}
