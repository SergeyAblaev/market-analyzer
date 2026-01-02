
package com.example.cryptoanalyzer.market.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
                            JsonNode node = mapper.readTree(data.toString()).get("data");
                            TradeEvent event = new TradeEvent(
                                    node.get("s").asText(),
                                    new BigDecimal(node.get("p").asText()),
                                    new BigDecimal(node.get("q").asText()),
                                    node.get("T").asLong()
                            );
                            if (consumer != null) consumer.accept(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        webSocket.request(1);
                        return null;
                    }
                });
    }
}
