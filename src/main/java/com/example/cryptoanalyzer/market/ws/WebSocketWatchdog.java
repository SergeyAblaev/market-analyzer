package com.example.cryptoanalyzer.market.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketWatchdog {

    private final BinanceWebSocketClient client;

    public WebSocketWatchdog(BinanceWebSocketClient client) {
        this.client = client;
    }

    @Scheduled(fixedDelay = 30_000)
    public void check() {
        if (client.silenceDuration().toMinutes() >= 2) {
            log.warn("No WS data for 2 minutes – restarting WS");
            client.restart();
        }
    }
}

