
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alert.MacOsAlertService;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;

import java.math.BigDecimal;
import java.util.Map;

public class PriceThresholdRule implements AlertRule {

    private final Map<String, Threshold> thresholds;
    private final MacOsAlertService alertService;

    public record Threshold(BigDecimal upper, BigDecimal lower) {}

    public PriceThresholdRule(Map<String, Threshold> thresholds,
                              MacOsAlertService alertService) {
        this.thresholds = thresholds;
        this.alertService = alertService;
    }

    @Override
    public void evaluate(OhlcCandle candle) {
        Threshold t = thresholds.get(candle.getSymbol().toLowerCase());
        if (t == null) return;

        if (t.upper() != null && candle.getClosePrice().compareTo(t.upper()) > 0) {
            alertService.notify("Price Alert",
                    candle.getSymbol() + " above " + t.upper());
        }

        if (t.lower() != null && candle.getClosePrice().compareTo(t.lower()) < 0) {
            alertService.notify("Price Alert",
                    candle.getSymbol() + " below " + t.lower());
        }
    }
}
