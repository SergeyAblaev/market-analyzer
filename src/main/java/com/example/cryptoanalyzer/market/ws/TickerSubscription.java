package com.example.cryptoanalyzer.market.ws;

import lombok.Getter;

public class TickerSubscription {
    @Getter
    private final String symbol;
    @Getter
    private final MarketType marketType;

    public TickerSubscription(String symbol, MarketType marketType) {
        this.symbol = symbol.toLowerCase();
        this.marketType = marketType;
    }

    public String getStreamName() {
        return symbol + "@trade";
    }
}
