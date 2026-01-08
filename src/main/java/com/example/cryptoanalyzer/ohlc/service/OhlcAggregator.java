
package com.example.cryptoanalyzer.ohlc.service;

import com.example.cryptoanalyzer.market.ws.TradeEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.ohlc.repository.OhlcCandleRepository;
import com.example.cryptoanalyzer.rules.RuleEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OhlcAggregator {

    private final OhlcCandleRepository repository;
    private final RuleEngineService ruleEngineService;

    @Value("${ohlc.timeframes}")
    private List<Integer> timeframes;

    private final Map<String, Map<Integer, OhlcCandle>> active = new HashMap<>();

    /**
     * A candle "closes" only when a trade related to the **next** time interval arrives.
     * Until then, the candle is considered "active" and exists only in memory (`Map active`)
     * so that new trades can be added to it and `highPrice```lowPrice``volume` can be updated.
     * @param trade
     * @return
     */
    public synchronized Optional<List<OhlcCandle>> onTrade(TradeEvent trade) {
        List<OhlcCandle> closed = new ArrayList<>(); //но тогда надо для 'closed' организовать хранилище в памяти?..
        for (int tf : timeframes) {
            active.computeIfAbsent(trade.symbol(), s -> new HashMap<>());
            Map<Integer, OhlcCandle> byTf = active.get(trade.symbol());

            long bucket = trade.tradeTime() / 1000 / tf * tf;
            Instant start = Instant.ofEpochSecond(bucket);
            Instant end = start.plusSeconds(tf);

            OhlcCandle candle = byTf.get(tf);
            if (candle == null || !candle.getStartTime().equals(start)) {
                if (candle != null) {
                    repository.save(candle);            //save closed candle
                    ruleEngineService.process(candle);  //run alerts
                    closed.add(candle); //свеча «закрывается» только тогда, когда приходит сделка, относящаяся к **следующему** временному интервалу. До этого момента свеча считается «активной» и живет только в памяти (`Map active`), чтобы в нее можно было добавлять новые сделки и обновлять , и . `highPrice``lowPrice``volume`
                }

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
        return Optional.ofNullable(closed).filter(c -> !c.isEmpty());
    }
}
