package com.mysite.sbb.stock;

import java.util.Map;
import java.util.Set;
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

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public StockWebSocketHandler(KisRealtimeClient kisClient) {
        this.kisClient = kisClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        try {
            session.sendMessage(new TextMessage("{\"type\":\"ACK\",\"code\":\"005930\"}"));
            log.info("[WS] ACK sent. session={}, open={}", session.getId(), session.isOpen());

            log.info("[WS] recv message: {}", message.getPayload());

            Map<String, Object> req = om.readValue(message.getPayload(), Map.class);
            String type = (String) req.get("type");

            if ("SUBSCRIBE".equals(type)) {
                String code = (String) req.get("code");

                kisClient.subscribe(code, price -> broadcastPrice(price));
            }

        } catch (Exception e) {
            log.error("[WS] handleTextMessage error", e);
            try {
                session.sendMessage(
                    new TextMessage("{\"type\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}")
                );
            } catch (Exception ignore) {}
        }
    }


    private void broadcastPrice(String price) {
        try {
            String json = om.writeValueAsString(Map.of("type", "PRICE", "price", price));
            TextMessage msg = new TextMessage(json);

            for (WebSocketSession s : sessions) {
                if (s.isOpen()) s.sendMessage(msg);
            }
        } catch (Exception ignored) {}
    }
}
