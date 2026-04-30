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
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BinanceWebSocketClient {

    public static final String TICKER_NOT_EXIST = "Ticker not exist";
    public static final String TICKER_ALREADY_EXISTS = "Ticker already exists";
    @Value("${binance.spot_ws-url}")
    private String spotWsUrl;

    @Value("${binance.futures_ws-url}")
    private String futuresWsUrl;

    @Value("${binance.spot-symbols}")
    private List<String> spotSymbols;

    @Value("${binance.futures-symbols}")
    private List<String> futuresSymbols;

    private static final int SHARD_SIZE = 50;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);

    private final List<WebSocket> spotSockets = new CopyOnWriteArrayList<>(); //Fixme: rewrite pools on 'ConcurrentHashMap'
    private final List<WebSocket> futuresSockets = new CopyOnWriteArrayList<>(); //Fixme: rewrite pools on 'ConcurrentHashMap'

    private final Map<WebSocket, Integer> reconnectAttempts = new ConcurrentHashMap<>();

    private final List<TickerSubscription> subscriptions = new CopyOnWriteArrayList<>();

    private volatile Instant lastMessageTime = Instant.now();

    private Consumer<TradeEvent> consumer;

    public void subscribe(Consumer<TradeEvent> consumer) {
        this.consumer = consumer;
    }

    // Format:
    //{"stream":"btcusdt@trade","data":{"e":"trade","E":1767387168240,"s":"BTCUSDT","t":5729884604,"p":"89690.38000000","q":"0.00006000","T":1767387168237,"m":true,"M":true}}
    //{"stream":"ethusdt@trade","data":{"e":"trade","E":1767387169112,"s":"ETHUSDT","t":3408559518,"p":"3117.25000000","q":"0.00170000","T":1767387169112,"m":true,"M":true}}
    @PostConstruct
    public void init() {
        loadInitialSubscriptions();
        connectAll();
        startHeartbeat();
    }

    private void loadInitialSubscriptions() {
        spotSymbols.forEach(s ->
                subscriptions.add(new TickerSubscription(s, MarketType.SPOT)));

        futuresSymbols.forEach(s ->
                subscriptions.add(new TickerSubscription(s, MarketType.FUTURES)));
    }

    // =========================
    // Ticker API
    // =========================
    public synchronized String addTicker(String symbol, MarketType type) {

        log.info("Adding ticker {} ({})", symbol, type);

        boolean exists = subscriptions.stream()
                .anyMatch(s -> s.getSymbol().equalsIgnoreCase(symbol));

        if (exists) {
            log.warn(TICKER_ALREADY_EXISTS + ": {}", symbol);
            return TICKER_ALREADY_EXISTS;
        }

        subscriptions.add(new TickerSubscription(symbol, type));

        restart();
        return "Ticker added";
    }

    public synchronized String removeTicker(String symbol, MarketType type) {

        log.info("Removing ticker {} ({})", symbol, type);

        boolean removed = subscriptions.removeIf(s ->
                s.getSymbol().equalsIgnoreCase(symbol)
                        && s.getMarketType() == type
        );

        if (!removed) {
            log.warn(TICKER_NOT_EXIST + ": {} ({})", symbol, type);
            return TICKER_NOT_EXIST;
        }
        restart();
        return "Ticker removed";
    }

    // =========================
    // CONNECT
    // =========================
    public synchronized void connectAll() {

        Map<MarketType, List<TickerSubscription>> grouped =
                subscriptions.stream()
                        .collect(Collectors.groupingBy(TickerSubscription::getMarketType));

        connectMarket(grouped.get(MarketType.SPOT), spotWsUrl, spotSockets);
        connectMarket(grouped.get(MarketType.FUTURES), futuresWsUrl, futuresSockets);
    }

    private void connectMarket(List<TickerSubscription> subs,
                               String url,
                               List<WebSocket> pool) {

        if (subs == null || subs.isEmpty()) return;

        List<List<TickerSubscription>> shards = shard(subs);

        for (List<TickerSubscription> shard : shards) {

            String streams = shard.stream()
                    .map(TickerSubscription::getStreamName)
                    .collect(Collectors.joining("/"));

            URI uri = URI.create(url + "?streams=" + streams);

            log.info("Connecting WS shard: {}", uri);

            httpClient.newWebSocketBuilder()
                    .buildAsync(uri, new Listener(shard, url, pool))
                    .thenAccept(ws -> {
                        pool.add(ws);
                        reconnectAttempts.put(ws, 0);
                    });
        }
    }

    private List<List<TickerSubscription>> shard(List<TickerSubscription> list) {
        List<List<TickerSubscription>> result = new ArrayList<>();

        for (int i = 0; i < list.size(); i += SHARD_SIZE) {
            result.add(list.subList(i, Math.min(i + SHARD_SIZE, list.size())));
        }

        return result;
    }

    // =========================
    // RECONNECT
    // =========================
    private void reconnect(WebSocket ws,
                           List<TickerSubscription> shard,
                           String url,
                           List<WebSocket> pool) {

        int attempt = reconnectAttempts.getOrDefault(ws, 0);

        if (attempt >= MAX_RECONNECT_ATTEMPTS) {
            log.error("Max reconnect attempts reached");
            return;
        }

        long delay = (long) Math.min(60, Math.pow(2, attempt)) * 1000
                + ThreadLocalRandom.current().nextLong(1000);

        reconnectAttempts.put(ws, attempt + 1);

        scheduler.schedule(() -> {
            log.warn("Reconnecting WS (attempt {})", attempt);

            connectMarket(shard, url, pool);

        }, delay, TimeUnit.MILLISECONDS);
    }

    // =========================
    // HEARTBEAT
    // =========================
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            spotSockets.forEach(ws -> ws.sendPing(ByteBuffer.wrap(new byte[]{1})));
            futuresSockets.forEach(ws -> ws.sendPing(ByteBuffer.wrap(new byte[]{1})));
        }, 30, 30, TimeUnit.SECONDS);
    }

    public Duration silenceDuration() {
        return Duration.between(lastMessageTime, Instant.now());
    }

    public synchronized void restart() {
        log.warn("Restarting ALL WS"); //FixMe: replace this from Restarting ALL WS -> Restarting ONE shard !
        closeAll();
        connectAll();
    }

    public synchronized void closeAll() {
        spotSockets.forEach(WebSocket::abort);
        futuresSockets.forEach(WebSocket::abort);

        spotSockets.clear();
        futuresSockets.clear();
    }

    // =========================
    // WebSocket Listener
    // =========================
    private class Listener implements WebSocket.Listener {

        private final List<TickerSubscription> shard;
        private final String url;
        private final List<WebSocket> pool;

        private final StringBuilder buffer = new StringBuilder();

        public Listener(List<TickerSubscription> shard,
                        String url,
                        List<WebSocket> pool) {
            this.shard = shard;
            this.url = url;
            this.pool = pool;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("WS opened");
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
                }

            } catch (Exception e) {
                log.error("WS parse error", e);
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("WS closed: {}", reason);
            pool.remove(webSocket);
            reconnect(webSocket, shard, url, pool);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("WS error", error);
            reconnect(webSocket, shard, url, pool);
        }
    }
}
