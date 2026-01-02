
package com.example.cryptoanalyzer.market.ws;

import java.math.BigDecimal;

public record TradeEvent(
        String symbol,
        BigDecimal price,
        BigDecimal quantity,
        long tradeTime
) {}
