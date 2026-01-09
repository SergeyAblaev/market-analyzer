
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PriceThresholdRule implements AlertRule {

    private final Map<String, Threshold> thresholds;
    private Map<String, Boolean> allertFlagsUp = new HashMap<>();
    private Map<String, Boolean> allertFlagsLow = new HashMap<>();

    public record Threshold(BigDecimal upper, BigDecimal lower) {
    }

    public PriceThresholdRule(Map<String, Threshold> thresholds) {
        this.thresholds = thresholds;
    }

//    @Override
//    public void evaluateMacOs(OhlcCandle candle) {
//        Threshold t = thresholds.get(candle.getSymbol().toLowerCase());
//        if (t == null) return;
//
//        if (t.upper() != null && candle.getClosePrice().compareTo(t.upper()) > 0) {
//            alertService.notify("Price Alert",
//                    candle.getSymbol() + " above " + t.upper());
//        }
//
//        if (t.lower() != null && candle.getClosePrice().compareTo(t.lower()) < 0) {
//            alertService.notify("Price Alert",
//                    candle.getSymbol() + " below " + t.lower());
//        }
//    }

    @Override
    public Optional<AlertEvent> evaluate(OhlcCandle candle) {
        String symbol = candle.getSymbol().toLowerCase();
        Threshold t = thresholds.get(symbol);
        if (t == null) return Optional.empty();

        BigDecimal close = candle.getClosePrice();
        if (t.upper() != null && close.compareTo(t.upper()) > 0) {
            boolean allertFlagUp = allertFlagsUp.getOrDefault(symbol, false);
            if (!allertFlagUp) {
                allertFlagsUp.put(symbol, true);
                String msg = candle.getSymbol() + " crossed above " + t.upper();
                return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PRICE_THRESHOLD", msg));
            }
        } else {
            allertFlagsUp.put(symbol, false);
        }
        if (t.lower() != null && close.compareTo(t.lower()) < 0) {
            boolean allertFlagLow = allertFlagsLow.getOrDefault(symbol, false);
            if (!allertFlagLow) {
                allertFlagsLow.put(symbol, true);
                String msg = candle.getSymbol() + " dropped below " + t.lower();
                return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PRICE_THRESHOLD", msg));
            }
        } else {
            allertFlagsLow.put(symbol, false);
        }
        return Optional.empty();
    }

}
