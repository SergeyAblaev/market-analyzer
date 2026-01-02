
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alert.MacOsAlertService;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;

import java.math.BigDecimal;
import java.util.*;

public class PercentChangeRule implements AlertRule {

    private final int candles;
    private final BigDecimal percent;
    private final int timeframe;
    private final MacOsAlertService alertService;

    private final Map<String, Deque<OhlcCandle>> history = new HashMap<>();

    public PercentChangeRule(int candles,
                             BigDecimal percent,
                             int timeframe,
                             MacOsAlertService alertService) {
        this.candles = candles;
        this.percent = percent;
        this.timeframe = timeframe;
        this.alertService = alertService;
    }

    @Override
    public void evaluate(OhlcCandle candle) {
        if (candle.getTimeframeSeconds() != timeframe) return;

        history.computeIfAbsent(candle.getSymbol(), k -> new ArrayDeque<>());
        Deque<OhlcCandle> deque = history.get(candle.getSymbol());

        deque.addLast(candle);
        if (deque.size() > candles) deque.removeFirst();

        if (deque.size() == candles) {
            OhlcCandle first = deque.getFirst();
            BigDecimal change = candle.getClosePrice()
                    .subtract(first.getClosePrice())
                    .divide(first.getClosePrice(), BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (change.abs().compareTo(percent) >= 0) {
                alertService.notify(
                        "Momentum Alert",
                        candle.getSymbol() + " changed " + change + "%"
                );
                deque.clear();
            }
        }
    }
}
