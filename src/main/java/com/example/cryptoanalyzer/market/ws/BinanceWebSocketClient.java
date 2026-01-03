
package com.example.cryptoanalyzer.market.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.math.BigDecimal;

@Component
@Slf4j
public class BinanceWebSocketClient {

    @Value("${binance.ws-url}")
    private String wsUrl;

    @Value("${binance.symbols}")
    private List<String> symbols;

    private final ObjectMapper mapper = new ObjectMapper();
    private Consumer<TradeEvent> consumer;

    public void subscribe(Consumer<TradeEvent> consumer) {
        this.consumer = consumer;
    }

    //{"stream":"btcusdt@trade","data":{"e":"trade","E":1767387168240,"s":"BTCUSDT","t":5729884604,"p":"89690.38000000","q":"0.00006000","T":1767387168237,"m":true,"M":true}}
    //{"stream":"ethusdt@trade","data":{"e":"trade","E":1767387169112,"s":"ETHUSDT","t":3408559518,"p":"3117.25000000","q":"0.00170000","T":1767387169112,"m":true,"M":true}}
    @PostConstruct
    public void connect() {
        String streams = symbols.stream()
                .map(s -> s + "@trade")
                .reduce((a, b) -> a + "/" + b)
                .orElseThrow();

        URI uri = URI.create(wsUrl + "?streams=" + streams);

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        try {
                            TradeEvent event;
                            log.debug("Received WS message: {}", data);
                            JsonNode node = mapper.readTree(data.toString()).get("data");
                            if (node != null) {
                                event = new TradeEvent(
                                        node.get("s").asText(),
                                        new BigDecimal(node.get("p").asText()),
                                        new BigDecimal(node.get("q").asText()),
                                        node.get("T").asLong()
                                );
                                if (consumer != null) consumer.accept(event);
                            } else {
                                log.warn("Invalid WS message: {}", data);
                            }
                        } catch (Exception e) {
                            log.error("Error parsing WS message: {}", data, e);
                        }
                        webSocket.request(1);
                        return null;
                    }
                });
    }
}
