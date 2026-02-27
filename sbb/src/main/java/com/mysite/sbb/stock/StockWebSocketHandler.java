package com.mysite.sbb.stock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.sbb.kis.KisRealtimeClient;

@Component
@Slf4j
public class StockWebSocketHandler extends TextWebSocketHandler {

    private final KisRealtimeClient kisClient;
    private final ObjectMapper om = new ObjectMapper();
    private final Map<WebSocketSession, KisRealtimeClient.StockPriceListener> sessionToListener = new ConcurrentHashMap<>();

    // ✅ 세션별 구독 종목
    private final Map<WebSocketSession, String> sessionToCode = new ConcurrentHashMap<>();

    public StockWebSocketHandler(KisRealtimeClient kisClient) {
        this.kisClient = kisClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[WS] connected session={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String oldCode = sessionToCode.remove(session);
        KisRealtimeClient.StockPriceListener listener = sessionToListener.remove(session);

        if (oldCode != null && listener != null) {
            try {
                kisClient.unsubscribe(oldCode, listener);
            } catch (Exception e) {
                log.warn("[WS] unsubscribe fail session={} code={}", session.getId(), oldCode, e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> req = om.readValue(message.getPayload(), Map.class);
            String type = (String) req.get("type");

            if ("SUBSCRIBE".equals(type)) {
                String code = (String) req.get("code");
                if (code == null || code.isBlank()) {
                    session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"code is required\"}"));
                    return;
                }

                String oldCode = sessionToCode.get(session);
                KisRealtimeClient.StockPriceListener oldListener = sessionToListener.get(session);

                if (oldCode != null && oldListener != null && !oldCode.equals(code)) {
                    kisClient.unsubscribe(oldCode, oldListener);
                }

                // 새 listener 생성해서 저장
                KisRealtimeClient.StockPriceListener newListener = price -> sendPriceToSession(session, code, price);
                sessionToCode.put(session, code);
                sessionToListener.put(session, newListener);

                kisClient.subscribe(code, newListener);
  
               
            }
        } catch (Exception e) {
            log.error("[WS] handleTextMessage error", e);
            try {
                session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}"));
            } catch (Exception ignore) {}
        }
    }

    private void sendPriceToSession(WebSocketSession session, String code, String price) {
        try {
            // ✅ 세션이 중간에 다른 종목으로 바뀌었으면 무시
            String currentCode = sessionToCode.get(session);
            if (currentCode == null || !currentCode.equals(code)) return;

            if (!session.isOpen()) return;

            String json = om.writeValueAsString(Map.of(
                    "type", "PRICE",
                    "code", code,
                    "price", price
            ));
            session.sendMessage(new TextMessage(json));
        } catch (Exception ignored) {}
    }
}
