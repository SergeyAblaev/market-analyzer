
package com.example.cryptoanalyzer.market.service;

import com.example.cryptoanalyzer.market.ws.BinanceWebSocketClient;
import com.example.cryptoanalyzer.market.ws.TradeEvent;
import com.example.cryptoanalyzer.ohlc.service.OhlcAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final BinanceWebSocketClient client;
    private final OhlcAggregator aggregator;
    private final ApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeToMarketData() {
        MarketDataService proxy = applicationContext.getBean(MarketDataService.class);
        client.subscribe(proxy::processTrade);
        log.info("Subscribed to market data via context proxy");
    }

    @Async
    public void processTrade(TradeEvent trade) {

        try {
        aggregator.onTrade(trade).ifPresent(c -> log.debug("Closed candles: {}", c));
        } catch (InvalidDataAccessResourceUsageException ex) {
            log.error("CRITICAL DB ERROR: Initiating an APPLICATION RESTART!...", ex);
            // Simple exit (It works well in case of restart policy having in Docker/K8s)
            System.exit(1);
        }
    }
}
