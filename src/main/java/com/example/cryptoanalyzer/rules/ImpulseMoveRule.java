package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.model.AlertDirection;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.web.model.AlertRuleUpdateDto;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

@Slf4j
public class ImpulseMoveRule implements AlertRule {

    public static final String ID = "IMPULSE_MOVE";

    private final String symbol;
    private final int timeframe;

    /* ===== configurable params ===== */
    // "{ 'BTCUSDT': { 'timeframe': 60, 'directionRatio': 0.75, 'minTotalMovePercent': 0.1, 'accelerationFactor': 1.2 },'ETHUSDT': { 'timeframe': 60, 'directionRatio': 0.75, 'minTotalMovePercent': 0.8, 'accelerationFactor': 1.5 } }"

    private int windowSize = 12;                    // minutes
    private BigDecimal directionRatio; // = new BigDecimal("0.75");
    private BigDecimal minTotalMovePercent; // = new BigDecimal("0.8");
    private BigDecimal accelerationFactor; // = new BigDecimal("1.5");

    /* ===== state ===== */

    private final Deque<OhlcCandle> window = new ArrayDeque<>();

    public ImpulseMoveRule(String symbol, int timeframe, BigDecimal directionRatio, BigDecimal minTotalMovePercent, BigDecimal accelerationFactor) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.directionRatio = directionRatio;
        this.minTotalMovePercent = minTotalMovePercent;
        this.accelerationFactor = accelerationFactor;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Optional<AlertEvent> evaluate(OhlcCandle candle) {

        window.addLast(candle);
        if (window.size() > windowSize) {
            window.removeFirst();
        }

        if (window.size() < windowSize) {
            return Optional.empty();
        }

        DirectionStats direction = calculateDirection();
        if (!direction.confirmed) {
            log.debug("[IMPULSE_MOVE] direction rejected for {}", symbol);
            return Optional.empty();
        }

        BigDecimal totalMove = calculateTotalMovePercent(direction.direction);
        if (totalMove.abs().compareTo(minTotalMovePercent) < 0) {
            log.debug("[IMPULSE_MOVE] magnitude rejected for {} ({}%)", symbol, totalMove);
            return Optional.empty();
        }

        if (!checkAcceleration(direction.direction)) {
            log.debug("[IMPULSE_MOVE] acceleration rejected for {}", symbol);
            return Optional.empty();
        }
        // Confidence (0–100)
        int confidence = calculateConfidence(direction, totalMove);

        // Calc vol for AlertEvent (1-8). And Round up:
        int vol = (int) Math.ceil((8.0 / 100.0) * confidence);
        AlertEvent event = new AlertEvent(
                symbol,
                timeframe,
                ID,
                buildMessage(direction.direction, totalMove, confidence),
                direction.direction,
                vol
        );

        log.warn("[IMPULSE_MOVE] ALERT {} {} confidence={}%", symbol, direction.direction, confidence);
        return Optional.of(event);
    }

    /* ===================================================== */

    private DirectionStats calculateDirection() {

        int bullish = 0;
        int bearish = 0;

        for (OhlcCandle c : window) {
            int cmp = c.getClosePrice().compareTo(c.getOpenPrice());
            if (cmp > 0) bullish++;
            else if (cmp < 0) bearish++;
        }

        int total = bullish + bearish;
        if (total == 0) {
            return DirectionStats.none();
        }

        BigDecimal bullishRatio = BigDecimal.valueOf(bullish)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

        BigDecimal bearishRatio = BigDecimal.valueOf(bearish)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

        if (bullishRatio.compareTo(directionRatio) >= 0) {
            return DirectionStats.bullish(bullishRatio);
        }

        if (bearishRatio.compareTo(directionRatio) >= 0) {
            return DirectionStats.bearish(bearishRatio);
        }

        return DirectionStats.none();
    }

    private BigDecimal calculateTotalMovePercent(AlertDirection direction) {

        OhlcCandle first = window.getFirst();
        OhlcCandle last = window.getLast();

        BigDecimal from = direction == AlertDirection.UP
                ? first.getOpenPrice()
                : last.getOpenPrice();

        BigDecimal to = direction == AlertDirection.UP
                ? last.getClosePrice()
                : first.getClosePrice();

        return to.subtract(from)
                .divide(from, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private boolean checkAcceleration(AlertDirection direction) {

        int half = windowSize / 2;

        BigDecimal firstHalfMove = BigDecimal.ZERO;
        BigDecimal secondHalfMove = BigDecimal.ZERO;

        OhlcCandle prev = null;
        int index = 0;

        for (OhlcCandle c : window) {
            if (prev != null) {
                BigDecimal diff = c.getClosePrice().subtract(prev.getClosePrice()).abs();
                if (index < half) {
                    firstHalfMove = firstHalfMove.add(diff);
                } else {
                    secondHalfMove = secondHalfMove.add(diff);
                }
            }
            prev = c;
            index++;
        }

        if (firstHalfMove.compareTo(BigDecimal.ZERO) == 0) {
            return true; // резкий старт
        }

        return secondHalfMove.compareTo(
                firstHalfMove.multiply(accelerationFactor)
        ) >= 0;
    }

    private int calculateConfidence(DirectionStats direction, BigDecimal move) {

        BigDecimal directionScore = direction.ratio
                .divide(BigDecimal.ONE, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(40));

        BigDecimal magnitudeScore = move.abs()
                .divide(minTotalMovePercent, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(35));

        BigDecimal accelerationScore = BigDecimal.valueOf(25);

        return directionScore
                .add(magnitudeScore)
                .add(accelerationScore)
                .min(BigDecimal.valueOf(100))
                .intValue();
    }

    /**
     * ALERT: STRONG BULLISH IMPULSE / BEARISH IMPULSE
     */
    private String buildMessage(AlertDirection direction,
                                BigDecimal move,
                                int confidence) {

        return String.format(
                "IMPULSE MOVE %s: %.2f%% last %d candles (confidence %d%%)",
                direction,
                move,
                windowSize,
                confidence
        );
    }

    @Override
    public void updateFrom(AlertRuleUpdateDto source) {

        if (source.candles() != null) {
            this.windowSize = source.candles();
        }

        if (source.percent() != null) {
            this.minTotalMovePercent = source.percent();
        }

        if (source.directionRatio() != null) {
            this.directionRatio = source.directionRatio();
        }

        if (source.accelerationFactor() != null) {
            this.accelerationFactor = source.accelerationFactor();
        }

        log.info("[IMPULSE_MOVE] updated params: window={}, minMove={}%, dirRatio={}, accel={}",
                windowSize, minTotalMovePercent, directionRatio, accelerationFactor);
    }

    /* ===================================================== */

    private static class DirectionStats {

        final boolean confirmed;
        final AlertDirection direction;
        final BigDecimal ratio;

        private DirectionStats(boolean confirmed, AlertDirection direction, BigDecimal ratio) {
            this.confirmed = confirmed;
            this.direction = direction;
            this.ratio = ratio;
        }

        static DirectionStats bullish(BigDecimal ratio) {
            return new DirectionStats(true, AlertDirection.UP, ratio);
        }

        static DirectionStats bearish(BigDecimal ratio) {
            return new DirectionStats(true, AlertDirection.DOWN, ratio);
        }

        static DirectionStats none() {
            return new DirectionStats(false, null, BigDecimal.ZERO);
        }
    }
}
