
package com.example.cryptoanalyzer.ohlc.service;

import com.example.cryptoanalyzer.market.ws.TradeEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OhlcAggregator {

    @Value("${ohlc.timeframes}")
    private List<Integer> timeframes;

    private final Map<String, Map<Integer, OhlcCandle>> active = new HashMap<>();

    public synchronized Optional<OhlcCandle> onTrade(TradeEvent trade) {
        List<OhlcCandle> closed = new ArrayList<>();
        for (int tf : timeframes) {
            active.computeIfAbsent(trade.symbol(), s -> new HashMap<>());
            Map<Integer, OhlcCandle> byTf = active.get(trade.symbol());

            long bucket = trade.tradeTime() / 1000 / tf * tf;
            Instant start = Instant.ofEpochSecond(bucket);
            Instant end = start.plusSeconds(tf);

            OhlcCandle candle = byTf.get(tf);
            if (candle == null || !candle.getStartTime().equals(start)) {
                if (candle != null) closed.add(candle);

                candle = new OhlcCandle();
                candle.setSymbol(trade.symbol());
                candle.setTimeframeSeconds(tf);
                candle.setStartTime(start);
                candle.setEndTime(end);
                candle.setOpenPrice(trade.price());
                candle.setHighPrice(trade.price());
                candle.setLowPrice(trade.price());
                candle.setClosePrice(trade.price());
                candle.setVolume(trade.quantity());

                byTf.put(tf, candle);
            } else {
                candle.setClosePrice(trade.price());
                candle.setHighPrice(candle.getHighPrice().max(trade.price()));
                candle.setLowPrice(candle.getLowPrice().min(trade.price()));
                candle.setVolume(candle.getVolume().add(trade.quantity()));
            }
        }
        return closed.stream().findFirst();
    }
}
