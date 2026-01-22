package com.example.cryptoanalyzer.config;

import com.example.cryptoanalyzer.alerts.MacOsAlertNotifier;
import com.example.cryptoanalyzer.rules.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Configuration
public class AlertRulesConfig {

    @Value("#{${alerts.price-thresholds}}")
    private Map<String, Map<String, BigDecimal>> priceThresholds;

    @Value("#{${alerts.percent-change}}")
    private Map<String, Map<String, Object>> percentConfigs;

    @Value("#{${alerts.impulse-move}}")
    private Map<String, Map<String, Object>> impulseMoveConfigs;

    @Value("#{${rules.impulse-move.is-active}}")
    private boolean isActiveImpulseMoveRule;

//    @Autowired
//    private MacOsAlertNotifier alertService;

    @Bean
    public List<AlertRule> alertRules() {
        List<AlertRule> list = new ArrayList<>();

        // PriceThresholdRules
        Map<String, PriceThresholdRule.Threshold> thresholds = new HashMap<>();
        priceThresholds.forEach((symbol, vals) -> {
            thresholds.put(symbol.toLowerCase(),
                    new PriceThresholdRule.Threshold(vals.get("upper"), vals.get("lower")));
        });
        list.add(new PriceThresholdRule(thresholds));
        log.info("Added Price Threshol rule for price thresholds: {}", thresholds);

        // PercentChangeRules
        percentConfigs.forEach((symbol, cfg) -> {
            int tf = (Integer) cfg.get("timeframe");
            int c  = (Integer) cfg.get("candles");
            BigDecimal pct = new BigDecimal(cfg.get("percent").toString());
            list.add(new PercentChangeRule(symbol ,c, pct, tf));
            log.info("Added Percent Change rule for {} with percent change {} and timeframe {}", symbol, pct, tf);
        });

        // ImpulseMoveRules
        if (isActiveImpulseMoveRule) {
            impulseMoveConfigs.forEach((symbol, cfg) -> {
                int tf = (Integer) cfg.get("timeframe");
                BigDecimal directionRatio = BigDecimal.valueOf((Double) cfg.get("directionRatio"));
                BigDecimal minTotalMovePercent = BigDecimal.valueOf((Double) cfg.get("minTotalMovePercent"));
                BigDecimal accelerationFactor = BigDecimal.valueOf((Double) cfg.get("accelerationFactor"));
                list.add(new ImpulseMoveRule(symbol, tf, directionRatio, minTotalMovePercent, accelerationFactor));
                log.info("Added Impulse Move rule for {} with directionRatio {} minTotalMovePercent {} accelerationFactor {}  ", symbol, directionRatio, minTotalMovePercent, accelerationFactor);
            });
        }

        return list;
    }
}
