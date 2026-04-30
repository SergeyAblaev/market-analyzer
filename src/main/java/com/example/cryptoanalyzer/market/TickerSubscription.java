package com.example.cryptoanalyzer.market;

public class TickerSubscription {
    private final String symbol;
    private final MarketType marketType;

    public TickerSubscription(String symbol, MarketType marketType) {
        this.symbol = symbol.toLowerCase();
        this.marketType = marketType;
    }

    public String getStreamName() {
        return symbol + "@trade";
    }

    public MarketType getMarketType() {
        return marketType;
    }
}
