
package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.model.AlertDirection;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.web.model.AlertRuleUpdateDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Getter
public class PercentChangeRule implements AlertRule {

    private final int timeframe;
    private int candles;
    private BigDecimal percent;
    private final String symbol;

    private final Map<String, Deque<OhlcCandle>> history = new HashMap<>();

    public PercentChangeRule(String symbol,
                             int candles,
                             BigDecimal percent,
                             int timeframe) {
        this.symbol = symbol;
        this.candles = candles;
        this.percent = percent;
        this.timeframe = timeframe;
    }

    @Override
    public Optional<AlertEvent> evaluate(OhlcCandle candle) {
        if (candle.getTimeframeSeconds() != timeframe) return Optional.empty();
        if (!candle.getSymbol().equals(symbol)) return Optional.empty();

        var deque = history.computeIfAbsent(candle.getSymbol(), k -> new ArrayDeque<>());
        deque.addLast(candle);
        if (deque.size() > candles) deque.removeFirst();
        if (deque.size() < candles) return Optional.empty();

        var first = deque.peekFirst();
//        BigDecimal change = candle.getClosePrice().subtract(first.getClosePrice())
        BigDecimal hi = candle.getHighPrice().abs();
        BigDecimal low = candle.getLowPrice().abs();
        BigDecimal diffPrice =  hi.max(low);
        BigDecimal change = diffPrice.subtract(first.getClosePrice())
                .divide(first.getClosePrice(), BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        if (change.abs().compareTo(percent) >= 0) {
            String msg = String.format("%s changed %.2f%% over last %d candles",
                    candle.getSymbol(), change, candles);
            deque.clear(); // reset window
            AlertDirection direction = change.compareTo(BigDecimal.ZERO) > 0 ? AlertDirection.UP : AlertDirection.DOWN;
            log.warn(msg.concat(" direction ").concat(direction.toString()));
            return Optional.of(new AlertEvent(candle.getSymbol(), candle.getTimeframeSeconds(), "PERCENT_CHANGE", msg, direction));
        } else {
            if (log.isDebugEnabled()) {
                AlertDirection direction = change.compareTo(BigDecimal.ZERO) > 0 ? AlertDirection.UP : AlertDirection.DOWN;
                log.debug("Percent change {} is below threshold {} for {} direction {} ", change, percent, candle.getSymbol(), direction);
            }
        }
        return Optional.empty();
    }

    @Override
    public String getId() {
        return "PERCENT_CHANGE_".concat(symbol);
    }

    @Override
    public void updateFrom(AlertRuleUpdateDto source) {
        if (source.candles() != null) {
            this.candles = source.candles();
        }
        if (source.percent() != null) {
            this.percent = source.percent();
        }
    }
}
