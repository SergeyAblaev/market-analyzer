
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.model.AlertDirection;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.alerts.MacOsAlertNotifier;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;

import java.math.BigDecimal;
import java.util.*;

public class PercentChangeRule implements AlertRule {

    private final int timeframe;
    private final int candles;
    private final BigDecimal percent;

    private final Map<String, Deque<OhlcCandle>> history = new HashMap<>();

    public PercentChangeRule(int candles,
                             BigDecimal percent,
                             int timeframe) {
        this.candles = candles;
        this.percent = percent;
        this.timeframe = timeframe;
    }

//    @Override
//    public void evaluateMacOs(OhlcCandle candle) {
//        if (candle.getTimeframeSeconds() != timeframe) return;
//
//        history.computeIfAbsent(candle.getSymbol(), k -> new ArrayDeque<>());
//        Deque<OhlcCandle> deque = history.get(candle.getSymbol());
//
//        deque.addLast(candle);
//        if (deque.size() > candles) deque.removeFirst();
//
//        if (deque.size() == candles) {
//            OhlcCandle first = deque.getFirst();
//            BigDecimal change = candle.getClosePrice()
//                    .subtract(first.getClosePrice())
//                    .divide(first.getClosePrice(), BigDecimal.ROUND_HALF_UP)
//                    .multiply(BigDecimal.valueOf(100));
//
//            if (change.abs().compareTo(percent) >= 0) {
//                alertService.notify(
//                        "Momentum Alert",
//                        candle.getSymbol() + " changed " + change + "%"
//                );
//                deque.clear();
//            }
//        }
//    }

    @Override
    public Optional<AlertEvent> evaluate(OhlcCandle candle) {
        if (candle.getTimeframeSeconds() != timeframe) return Optional.empty();

        var deque = history.computeIfAbsent(candle.getSymbol(), k -> new ArrayDeque<>());
        deque.addLast(candle);
        if (deque.size() > candles) deque.removeFirst();
        if (deque.size() < candles) return Optional.empty();

        var first = deque.peekFirst();
        BigDecimal change = candle.getClosePrice().subtract(first.getClosePrice())
                .divide(first.getClosePrice(), BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (change.abs().compareTo(percent) >= 0) {
            String msg = String.format("%s changed %.2f%% over last %d candles",
                    candle.getSymbol(), change, candles);
            deque.clear(); // reset window
            return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PERCENT_CHANGE", msg, AlertDirection.NEUTRAL));
        }
        return Optional.empty();
    }
}
