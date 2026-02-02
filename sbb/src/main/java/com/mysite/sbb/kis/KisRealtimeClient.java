package com.mysite.sbb.kis;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KisRealtimeClient {

    @Value("${kis.ws-url}")
    private String kisWsUrl;

    private final KisApprovalService approvalService;
    private final ObjectMapper om = new ObjectMapper();

    private volatile WebSocketSession kisSession;

    // 코드별로 콜백을 달아두는 맵
    private final Map<String, StockPriceListener> listeners = new ConcurrentHashMap<>();

    // ✅ 중복 구독 방지
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();

    public KisRealtimeClient(KisApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * code 예: "005930"
     * listener: 현재가(String)만 전달
     */
    public synchronized void subscribe(String code, StockPriceListener listener) throws Exception {
        listeners.put(code, listener);

        if (kisSession == null || !kisSession.isOpen()) {
            connectIfNeeded();
        }

        // ✅ 이미 구독한 종목이면 다시 subscribe 보내지 않음
        if (!subscribed.add(code)) {
            return;
        }

        sendSubscribe(code);
    }

    /**
     * (선택) 페이지 닫힐 때 unsubscribe를 붙이고 싶으면 사용
     */
    public synchronized void unsubscribe(String code) throws Exception {
        listeners.remove(code);
        if (kisSession == null || !kisSession.isOpen()) return;

        if (subscribed.remove(code)) {
            sendUnsubscribe(code);
        }
    }

    private void connectIfNeeded() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        ListenableFuture<WebSocketSession> future =
                client.doHandshake(new KisHandler(), kisWsUrl); // ✅ 네 환경에 맞게 String 버전 사용
        kisSession = future.get();
    }

    private void sendSubscribe(String code) throws Exception {
        String approvalKey = approvalService.getApprovalKey();

        Map<String, Object> msg = Map.of(
                "header", Map.of(
                        "approval_key", approvalKey,
                        "custtype", "P",
                        "tr_type", "1",           // 등록
                        "content-type", "utf-8"
                ),
                "body", Map.of(
                        "input", Map.of(
                                "tr_id", "H0STCNT0", // 국내주식 실시간체결
                                "tr_key", code
                        )
                )
        );

        kisSession.sendMessage(new TextMessage(om.writeValueAsString(msg)));
    }

    private void sendUnsubscribe(String code) throws Exception {
        String approvalKey = approvalService.getApprovalKey();

        Map<String, Object> msg = Map.of(
                "header", Map.of(
                        "approval_key", approvalKey,
                        "custtype", "P",
                        "tr_type", "2",           // 해제
                        "content-type", "utf-8"
                ),
                "body", Map.of(
                        "input", Map.of(
                                "tr_id", "H0STCNT0",
                                "tr_key", code
                        )
                )
        );

        kisSession.sendMessage(new TextMessage(om.writeValueAsString(msg)));
    }

    private class KisHandler extends TextWebSocketHandler {

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();

            // 1) JSON 메시지(구독응답/에러/PINGPONG) 처리
            if (payload.startsWith("{")) {
                handleJsonControlMessage(session, payload);
                return;
            }

            // 2) 실시간 데이터 메시지 처리: "0|TRID|cnt|^...^..."
            if (payload.startsWith("0|")) {
                handleRealtimeData(payload);
                return;
            }

            // 그 외는 일단 로그만 (필요 시)
            // System.out.println("[KIS] unknown payload: " + payload);
        }

        private void handleJsonControlMessage(WebSocketSession session, String payload) throws Exception {
            JsonNode root = om.readTree(payload);
            String trId = root.path("header").path("tr_id").asText();

            // ✅ PINGPONG은 UI로 보내지 말고, pong만 응답
            if ("PINGPONG".equals(trId)) {
                session.sendMessage(new PongMessage(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8))));
                return;
            }

            // 구독 응답/에러 (예: ALREADY IN SUBSCRIBE)
            String rtCd = root.path("body").path("rt_cd").asText();
            String msg1 = root.path("body").path("msg1").asText();
            String msgCd = root.path("body").path("msg_cd").asText();

            // rt_cd가 "1"이면 보통 실패/에러 케이스
            // (이 메시지를 화면에 뿌리면 네가 겪은 것처럼 현재가 자리에서 JSON이 보임)
            System.out.println("[KIS][CONTROL] tr_id=" + trId + ", rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
        }

        private void handleRealtimeData(String payload) {
            // 형식: 0|H0STCNT0|1|<data>
            String[] parts = payload.split("\\|", -1);
            if (parts.length < 4) return;

            String trId = parts[1];
            if (!"H0STCNT0".equals(trId)) return;

            // parts[3] = '^'로 구분된 필드들
            String data = parts[3];
            String[] v = data.split("\\^", -1);

            // KIS 예제 menulist 기준:
            // 0: 유가증권단축종목코드
            // 1: 주식체결시간
            // 2: 주식현재가  <-- 우리가 뽑을 값
            if (v.length <= 2) return;

            String code = v[0];
            String currentPrice = v[2];

            // "005930" 구독했으면 여기로 값이 들어와야 함
            StockPriceListener listener = listeners.get(code);
            if (listener != null) {
                // ✅ 숫자만 콜백으로 넘김 (UI에는 숫자만 표시되게)
                listener.onPrice(currentPrice);
            }
        }
    }

    @FunctionalInterface
    public interface StockPriceListener {
        void onPrice(String price);
    }
}
