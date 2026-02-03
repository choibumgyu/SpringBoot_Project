package com.mysite.sbb.kis;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KisRealtimeClient {

    @Value("${kis.ws-url}")
    private String kisWsUrl;

    private final KisApprovalService approvalService;
    private final ObjectMapper om = new ObjectMapper();

    private volatile WebSocketSession kisSession;

    // 코드별 콜백
    private final Map<String, StockPriceListener> listeners = new ConcurrentHashMap<>();

    // 중복 구독 방지 + 재구독 대상
    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();

    // connect 중복 방지
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    // 재연결 전용 단일 스레드
    private final ScheduledExecutorService reconnectExec =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "kis-reconnect");
                t.setDaemon(true);
                return t;
            });

    // 재연결 백오프
    private volatile int reconnectDelaySec = 1;
    private static final int RECONNECT_DELAY_MAX_SEC = 60;

    public KisRealtimeClient(KisApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostConstruct
    public void init() {
        // 앱 시작 시 미리 연결 시도 (선택)
        // 실패해도 subscribe 때 다시 연결됨
        scheduleReconnect(0);
    }

    /**
     * code 예: "005930"
     * listener: 현재가(String)
     */
    public void subscribe(String code, StockPriceListener listener) throws Exception {
        listeners.put(code, listener);

        // 구독 목록에 추가(재연결 시 재구독용)
        boolean firstTime = subscribed.add(code);

        // 연결이 죽어있으면 연결 먼저
        ensureConnected();

        // 최초 구독일 때만 KIS로 subscribe 메시지 전송
        if (firstTime) {
            sendSubscribe(code);
            log.info("[KIS] subscribe sent. code={}", code);
        } else {
            // 이미 구독중이어도, 연결이 재수립된 직후라면 서버가 구독을 잃었을 수 있음
            // 안전하게 한 번 더 보내고 싶다면 아래 주석을 해제(중복구독 응답이 귀찮으면 유지)
            // sendSubscribe(code);
        }
    }

    public void unsubscribe(String code) throws Exception {
        listeners.remove(code);

        if (subscribed.remove(code)) {
            if (isConnected()) {
                sendUnsubscribe(code);
                log.info("[KIS] unsubscribe sent. code={}", code);
            }
        }
    }

    private boolean isConnected() {
        return kisSession != null && kisSession.isOpen();
    }

    private void ensureConnected() {
        if (isConnected()) return;

        // 동시에 여러 요청이 들어와도 connect는 한 번만
        if (!connecting.compareAndSet(false, true)) return;

        reconnectExec.execute(() -> {
            try {
                connect();
                reconnectDelaySec = 1; // 성공하면 백오프 리셋

                // 재연결되면 재구독
                resubscribeAll();

            } catch (Exception e) {
                log.error("[KIS] connect failed", e);
                scheduleReconnect(reconnectDelaySec);
                reconnectDelaySec = Math.min(reconnectDelaySec * 2, RECONNECT_DELAY_MAX_SEC);
            } finally {
                connecting.set(false);
            }
        });
    }

    private void scheduleReconnect(int delaySec) {
        reconnectExec.schedule(() -> {
            // 이미 연결되어 있으면 스킵
            if (isConnected()) return;
            ensureConnected();
        }, delaySec, TimeUnit.SECONDS);
    }

    private synchronized void connect() throws Exception {
        // 이미 연결되어 있으면 스킵
        if (isConnected()) return;

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(1024 * 1024);   // 1MB (필요시 2~4MB로 올려도 됨)
        container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);

        StandardWebSocketClient client = new StandardWebSocketClient(container);

        ListenableFuture<WebSocketSession> future =
                client.doHandshake(new KisHandler(), kisWsUrl);

        kisSession = future.get(10, TimeUnit.SECONDS); // 타임아웃
        log.info("[KIS] connected. sessionId={}", kisSession.getId());
    }

    private void resubscribeAll() {
        if (!isConnected()) return;

        // 서버가 재시작/연결끊김/세션 교체되면 기존 구독이 날아가므로 재구독
        for (String code : subscribed) {
            try {
                sendSubscribe(code);
                log.info("[KIS] resubscribe sent. code={}", code);
            } catch (Exception e) {
                log.error("[KIS] resubscribe failed. code={}", code, e);
            }
        }
    }

    private void sendSubscribe(String code) throws Exception {
        if (!isConnected()) throw new IllegalStateException("KIS session not connected");

        String approvalKey = approvalService.getApprovalKey();

        Map<String, Object> msg = Map.of(
                "header", Map.of(
                        "approval_key", approvalKey,
                        "custtype", "P",
                        "tr_type", "1",
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

    private void sendUnsubscribe(String code) throws Exception {
        if (!isConnected()) throw new IllegalStateException("KIS session not connected");

        String approvalKey = approvalService.getApprovalKey();

        Map<String, Object> msg = Map.of(
                "header", Map.of(
                        "approval_key", approvalKey,
                        "custtype", "P",
                        "tr_type", "2",
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
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("[KIS] afterConnectionEstablished. sessionId={}", session.getId());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("[KIS] connection closed. code={}, reason={}", status.getCode(), status.getReason());

            // 세션 무효화
            try { session.close(); } catch (Exception ignore) {}
            kisSession = null;

            // 재연결 예약
            scheduleReconnect(reconnectDelaySec);
            reconnectDelaySec = Math.min(reconnectDelaySec * 2, RECONNECT_DELAY_MAX_SEC);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("[KIS] transport error", exception);

            // 세션 무효화
            try { session.close(); } catch (Exception ignore) {}
            kisSession = null;

            // 재연결 예약
            scheduleReconnect(reconnectDelaySec);
            reconnectDelaySec = Math.min(reconnectDelaySec * 2, RECONNECT_DELAY_MAX_SEC);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();

            // 1) JSON 메시지 처리
            if (payload.startsWith("{")) {
                handleJsonControlMessage(session, payload);
                return;
            }

            // 2) 실시간 데이터 메시지
            if (payload.startsWith("0|")) {
                handleRealtimeData(payload);
                return;
            }
        }

        private void handleJsonControlMessage(WebSocketSession session, String payload) throws Exception {
            JsonNode root = om.readTree(payload);
            String trId = root.path("header").path("tr_id").asText();

            if ("PINGPONG".equals(trId)) {
                // 서버가 pingpong 주면 pong으로 응답
                session.sendMessage(new PongMessage(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8))));
                return;
            }

            String rtCd = root.path("body").path("rt_cd").asText();
            String msg1 = root.path("body").path("msg1").asText();
            String msgCd = root.path("body").path("msg_cd").asText();

            // rt_cd="1"이면 에러인 경우가 많음
            if ("1".equals(rtCd)) {
                log.warn("[KIS][CONTROL][ERR] tr_id={}, msg_cd={}, msg1={}", trId, msgCd, msg1);

                // 승인키 만료/구독 실패류면 재연결 트리거(안전빵)
                // msg_cd / msg1 기준으로 더 정교하게 분기해도 됨
                kisSession = null;
                scheduleReconnect(1);
            } else {
                log.info("[KIS][CONTROL] tr_id={}, msg_cd={}, msg1={}", trId, msgCd, msg1);
            }
        }

        private void handleRealtimeData(String payload) {
            String[] parts = payload.split("\\|", -1);
            if (parts.length < 4) return;

            String trId = parts[1];
            if (!"H0STCNT0".equals(trId)) return;

            String data = parts[3];
            String[] v = data.split("\\^", -1);

            if (v.length <= 2) return;

            String code = v[0];
            String currentPrice = v[2];

            StockPriceListener listener = listeners.get(code);
            if (listener != null) {
                listener.onPrice(currentPrice);
            }
        }
    }

    @FunctionalInterface
    public interface StockPriceListener {
        void onPrice(String price);
    }
}
