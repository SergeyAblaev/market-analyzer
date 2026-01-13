
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.model.AlertDirection;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.web.model.AlertRuleUpdateDto;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class PriceThresholdRule implements AlertRule {

    private final Map<String, Threshold> thresholds;
    private Map<String, Boolean> allertFlagsUp = new HashMap<>();
    private Map<String, Boolean> allertFlagsLow = new HashMap<>();

    public record Threshold(BigDecimal upper, BigDecimal lower) {
    }

    public PriceThresholdRule(Map<String, Threshold> thresholds) {
        this.thresholds = thresholds;
    }


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
                String msg = " crossed above " + t.upper() + " (" + close.intValue() + ")";
                return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PRICE_THRESHOLD", msg, AlertDirection.UP, 1));
            }
        } else {
            allertFlagsUp.put(symbol, false);
        }
        if (t.lower() != null && close.compareTo(t.lower()) < 0) {
            boolean allertFlagLow = allertFlagsLow.getOrDefault(symbol, false);
            if (!allertFlagLow) {
                allertFlagsLow.put(symbol, true);
                String msg = " dropped below " + t.lower() + " (" + close.intValue() + ")";
                return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PRICE_THRESHOLD", msg, AlertDirection.DOWN, 1));
            }
        } else {
            allertFlagsLow.put(symbol, false);
        }
        return Optional.empty();
    }

    @Override
    public String getId() {
        return "PriceThresholdRule";
    }

    @Override
    public void updateFrom(AlertRuleUpdateDto source) {
//        PriceThresholdRule s = (PriceThresholdRule) source;
        throw new RuntimeException("PriceThresholdRule update is not implemented!");
    }
}
