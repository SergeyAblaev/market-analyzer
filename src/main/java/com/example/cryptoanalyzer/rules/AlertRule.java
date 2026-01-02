
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;

public interface AlertRule {
    void evaluate(OhlcCandle candle);
}
