
package com.example.cryptoanalyzer.market.service;

import com.example.cryptoanalyzer.market.ws.BinanceWebSocketClient;
import com.example.cryptoanalyzer.ohlc.service.OhlcAggregator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MarketDataService {

    public MarketDataService(BinanceWebSocketClient client, OhlcAggregator aggregator) {
        client.subscribe(trade -> {
            aggregator.onTrade(trade)
                      .ifPresent(c -> log.debug("Closed candles: {}", c));
        });
    }

    @PostConstruct
    public void init() {
        // triggers WS client creation
    }
}
