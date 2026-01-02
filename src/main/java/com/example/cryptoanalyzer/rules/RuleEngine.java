
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleEngine {

    private final List<AlertRule> rules;

    public RuleEngine(List<AlertRule> rules) {
        this.rules = rules;
    }

    public void evaluate(OhlcCandle candle) {
        rules.forEach(r -> r.evaluate(candle));
    }
}
