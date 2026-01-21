package com.example.cryptoanalyzer.config;

import com.example.cryptoanalyzer.alerts.MacOsAlertNotifier;
import com.example.cryptoanalyzer.rules.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.*;

@Configuration
public class AlertRulesConfig {

    @Value("#{${alerts.price-thresholds}}")
    private Map<String, Map<String, BigDecimal>> priceThresholds;

    @Value("#{${alerts.percent-change}}")
    private Map<String, Map<String, Object>> percentConfigs;

    @Value("#{${alerts.impulse-move}}")
    private Map<String, Map<String, Object>> impulseMoveConfigs;

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

        // PercentChangeRules
        percentConfigs.forEach((symbol, cfg) -> {
            int tf = (Integer) cfg.get("timeframe");
            int c  = (Integer) cfg.get("candles");
            BigDecimal pct = new BigDecimal(cfg.get("percent").toString());
            list.add(new PercentChangeRule(symbol ,c, pct, tf));
        });

        // ImpulseMoveRules
        impulseMoveConfigs.forEach((symbol, cfg) -> {
            int tf = (Integer) cfg.get("timeframe");
            BigDecimal directionRatio  = BigDecimal.valueOf((Double) cfg.get("directionRatio"));
            BigDecimal minTotalMovePercent  = BigDecimal.valueOf((Double) cfg.get("minTotalMovePercent"));
            BigDecimal accelerationFactor  = BigDecimal.valueOf((Double) cfg.get("accelerationFactor"));
            list.add(new ImpulseMoveRule(symbol, tf,  directionRatio,  minTotalMovePercent,  accelerationFactor));
        });

        return list;
    }
}
