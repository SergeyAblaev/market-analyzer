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

    @Value("${binance.spot_ws-url}")
    private String spotWsUrl;

    @Value("${binance.futures_ws-url}")
    private String futuresWsUrl;

    @Value("${binance.spot-symbols}")
    private List<String> spotSymbols;

    @Value("${binance.futures-symbols}")
    private List<String> futuresSymbols;

    private final ObjectMapper mapper = new ObjectMapper();

    private volatile Instant lastSpotMessageTime = Instant.now();
    private volatile Instant lastFuturesMessageTime = Instant.now();

    private volatile WebSocket spotWebSocket;
    private volatile WebSocket futuresWebSocket;

    private Consumer<TradeEvent> consumer;

    public void subscribe(Consumer<TradeEvent> consumer) {
        this.consumer = consumer;
    }

    // Format:
    //{"stream":"btcusdt@trade","data":{"e":"trade","E":1767387168240,"s":"BTCUSDT","t":5729884604,"p":"89690.38000000","q":"0.00006000","T":1767387168237,"m":true,"M":true}}
    //{"stream":"ethusdt@trade","data":{"e":"trade","E":1767387169112,"s":"ETHUSDT","t":3408559518,"p":"3117.25000000","q":"0.00170000","T":1767387169112,"m":true,"M":true}}
    @PostConstruct
    public synchronized void connect() {

        if (spotWebSocket != null || futuresWebSocket != null) {
            log.warn("WebSockets already connected, skipping connect()");
            return;
        }

        if (!spotSymbols.isEmpty()) {
            connectSpot(spotSymbols);
        }

        if (!futuresSymbols.isEmpty()) {
            connectFutures(futuresSymbols);
        }
    }

    private void connectSpot(List<String> symbols) {
        String streams = buildStreams(symbols);
        URI uri = URI.create(spotWsUrl + "?streams=" + streams);

        log.info("Connecting to Binance SPOT WS: {}", uri);

        String webSocketType = "SPOT";
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener(webSocketType))
                .thenAccept(ws -> {
                    this.spotWebSocket = ws;
                    this.lastSpotMessageTime = Instant.now();
                    log.info("Binance SPOT WS connected");
                })
                .exceptionally(ex -> {
                    log.error("Failed to connect SPOT WS", ex);
                    return null;
                });
    }

    private void connectFutures(List<String> symbols) {
        String streams = buildStreams(symbols);
        URI uri = URI.create(futuresWsUrl + "?streams=" + streams);

        log.info("Connecting to Binance FUTURES WS: {}", uri);

        String webSocketType = "FUTURES";
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new Listener(webSocketType))
                .thenAccept(ws -> {
                    this.futuresWebSocket = ws;
                    this.lastFuturesMessageTime = Instant.now();
                    log.info("Binance FUTURES WS connected");
                })
                .exceptionally(ex -> {
                    log.error("Failed to connect FUTURES WS", ex);
                    return null;
                });
    }

    private String buildStreams(List<String> symbols) {
        return symbols.stream()
                .map(s -> s.toLowerCase() + "@trade")
                .reduce((a, b) -> a + "/" + b)
                .orElseThrow();
    }

    public Duration silenceDuration() {
        Instant last = lastSpotMessageTime.isBefore(lastFuturesMessageTime)
                ? lastSpotMessageTime
                : lastFuturesMessageTime;

        return Duration.between(last, Instant.now());
    }

    public synchronized void close() {

        if (spotWebSocket != null) {
            try {
                log.info("Closing SPOT WS");
                spotWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client restart").join();
            } catch (Exception e) {
                log.warn("Error closing SPOT WS", e);
            } finally {
                spotWebSocket = null;
            }
        }

        if (futuresWebSocket != null) {
            try {
                log.info("Closing FUTURES WS");
                futuresWebSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client restart").join();
            } catch (Exception e) {
                log.warn("Error closing FUTURES WS", e);
            } finally {
                futuresWebSocket = null;
            }
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

        private final String type;
        private final StringBuilder buffer = new StringBuilder();

        public Listener(String type) {
            this.type = type;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("Binance {} WS opened", type);
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
            buffer.setLength(0);

            if ("SPOT".equals(type)) {
                lastSpotMessageTime = Instant.now();
            } else {
                lastFuturesMessageTime = Instant.now();
            }

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
            log.warn("Binance {} WS closed: {} - {}", type, statusCode, reason);

            if ("SPOT".equals(type)) {
                spotWebSocket = null;
                lastSpotMessageTime = Instant.EPOCH;
            } else {
                futuresWebSocket = null;
                lastFuturesMessageTime = Instant.EPOCH;
            }

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Binance {} WS error", type, error);
        }
    }
}
