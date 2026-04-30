package com.example.cryptoanalyzer.config;

import com.example.cryptoanalyzer.alerts.MacOsAlertNotifier;
import com.example.cryptoanalyzer.rules.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AlertRulesConfig {

    private final AlertProperties alertProperties;

    @Value("#{${rules.impulse-move.is-active}}")
    private boolean isActiveImpulseMoveRule;

    @Bean
    public List<AlertRule> alertRules() {
        List<AlertRule> list = new ArrayList<>();

        // PriceThresholdRules
        Map<String, PriceThresholdRule.Threshold> thresholds = new HashMap<>();
        alertProperties.getPriceThresholds().forEach((symbol, vals) -> {
            thresholds.put(symbol.toLowerCase(),
                    new PriceThresholdRule.Threshold(vals.getUpper(), vals.getLower()));
        });
        list.add(new PriceThresholdRule(thresholds));
        log.info("Added Price Threshol rule for price thresholds: {}", thresholds);

        // PercentChangeRules
        alertProperties.getPercentChange().forEach((symbol, cfg) -> {
            int tf = (Integer) cfg.getTimeframe();
            int c  = (Integer) cfg.getCandles();
            BigDecimal pct = new BigDecimal(cfg.getPercent().toString());
            list.add(new PercentChangeRule(symbol ,c, pct, tf));
            log.info("Added Percent Change rule for {} with percent change {} and timeframe {}", symbol, pct, tf);
        });

        // ImpulseMoveRules
        if (isActiveImpulseMoveRule) {
            alertProperties.getImpulseMove().forEach((symbol, cfg) -> {
                int tf = (Integer) cfg.getTimeframe();
                BigDecimal directionRatio = BigDecimal.valueOf((Double) cfg.getDirectionRatio());
                BigDecimal minTotalMovePercent = BigDecimal.valueOf((Double) cfg.getMinTotalMovePercent());
                BigDecimal accelerationFactor = BigDecimal.valueOf((Double) cfg.getAccelerationFactor());
                list.add(new ImpulseMoveRule(symbol, tf, directionRatio, minTotalMovePercent, accelerationFactor));
                log.info("Added Impulse Move rule for {} with directionRatio {} minTotalMovePercent {} accelerationFactor {}  ", symbol, directionRatio, minTotalMovePercent, accelerationFactor);
            });
        }

        return list;
    }
}
