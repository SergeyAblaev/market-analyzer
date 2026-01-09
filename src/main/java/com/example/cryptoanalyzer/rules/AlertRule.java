
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;

import java.util.Optional;

public interface AlertRule {

    Optional<AlertEvent> evaluate(OhlcCandle candle);
}
