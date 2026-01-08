
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.MacOsAlertNotifier;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class PriceThresholdRule implements AlertRule {

    private final Map<String, Threshold> thresholds;
    private final MacOsAlertNotifier alertService;

    public record Threshold(BigDecimal upper, BigDecimal lower) {}

    public PriceThresholdRule(Map<String, Threshold> thresholds,
                              MacOsAlertNotifier alertService) {
        this.thresholds = thresholds;
        this.alertService = alertService;
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
        Threshold t = thresholds.get(candle.getSymbol().toLowerCase());
        if (t == null) return Optional.empty();

        BigDecimal close = candle.getClosePrice();
        if (t.upper() != null && close.compareTo(t.upper()) > 0) {
            String msg = candle.getSymbol() + " crossed above " + t.upper();
            return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PRICE_THRESHOLD", msg));
        }
        if (t.lower() != null && close.compareTo(t.lower()) < 0) {
            String msg = candle.getSymbol() + " dropped below " + t.lower();
            return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PRICE_THRESHOLD", msg));
        }

        return Optional.empty();
    }

}
