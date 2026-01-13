package com.example.cryptoanalyzer.market.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Component
@Slf4j
public class BinanceWebSocketClient {

    @Value("${binance.ws-url}")
    private String wsUrl;

    @Value("${binance.symbols}")
    private List<String> symbols;

    private final ObjectMapper mapper = new ObjectMapper();

    private volatile Instant lastMessageTime = Instant.now();
    private volatile WebSocket webSocket;
    private Consumer<TradeEvent> consumer;

    public void subscribe(Consumer<TradeEvent> consumer) {
        this.consumer = consumer;
    }

    // Format:
    //{"stream":"btcusdt@trade","data":{"e":"trade","E":1767387168240,"s":"BTCUSDT","t":5729884604,"p":"89690.38000000","q":"0.00006000","T":1767387168237,"m":true,"M":true}}
    //{"stream":"ethusdt@trade","data":{"e":"trade","E":1767387169112,"s":"ETHUSDT","t":3408559518,"p":"3117.25000000","q":"0.00170000","T":1767387169112,"m":true,"M":true}}
    @PostConstruct
    public synchronized void connect() {
        if (webSocket != null) {
            log.warn("WebSocket already connected, skipping connect()");
            return;
        }

        String streams = symbols.stream()
                .map(s -> s.toLowerCase() + "@trade")
                .reduce((a, b) -> a + "/" + b)
                .orElseThrow();

        URI uri = URI.create(wsUrl + "?streams=" + streams);

        log.info("Connecting to Binance WS: {}", uri);

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    this.lastMessageTime = Instant.now();
                    log.info("Binance WS connected");
                })
                .exceptionally(ex -> {
                    log.error("Failed to connect Binance WS", ex);
                    return null;
                });
    }

    public Duration silenceDuration() {
        return Duration.between(lastMessageTime, Instant.now());
    }

    public synchronized void close() {
        if (webSocket == null) {
            return;
        }

        try {
            log.info("Closing Binance WS");
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client restart").join();
        } catch (Exception e) {
            log.warn("Error closing WS", e);
        } finally {
            webSocket = null;
        }
    }

    public synchronized void restart() {
        log.warn("Restarting Binance WS");
        close();
        connect();
    }

    // =========================
    // WebSocket Listener
    // =========================
    private class Listener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("Binance WS opened");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (!last) {
                webSocket.request(1);
                return null;
            }

            String fullMessage = buffer.toString();
            buffer.setLength(0); // clean buffer for the next message
            lastMessageTime = Instant.now();

            try {
                JsonNode node = mapper.readTree(fullMessage).get("data");
                if (node != null) {
                    TradeEvent event = new TradeEvent(
                            node.get("s").asText(),
                            new BigDecimal(node.get("p").asText()),
                            new BigDecimal(node.get("q").asText()),
                            node.get("T").asLong()
                    );
                    if (consumer != null) {
                        consumer.accept(event);
                    }
                } else {
                    log.warn("Invalid WS message: {}", fullMessage);
                }
            } catch (Exception e) {
                log.error("Error parsing WS message: {}", fullMessage, e);
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("Binance WS closed: {} - {}", statusCode, reason);
            BinanceWebSocketClient.this.webSocket = null;
            lastMessageTime = Instant.EPOCH; // Reset time for the fastest response 'WebSocketWatchdog'
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Binance WS error", error);
        }
    }
}
