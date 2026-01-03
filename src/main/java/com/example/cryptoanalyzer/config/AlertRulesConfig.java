package com.example.cryptoanalyzer.config;

import com.example.cryptoanalyzer.alerts.service.MacOsAlertService;
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

    @Autowired
    private MacOsAlertService alertService;

    @Bean
    public List<AlertRule> alertRules() {
        List<AlertRule> list = new ArrayList<>();

        Map<String, PriceThresholdRule.Threshold> thresholds = new HashMap<>();
        priceThresholds.forEach((sym, vals) -> {
            thresholds.put(sym.toLowerCase(),
                    new PriceThresholdRule.Threshold(vals.get("upper"), vals.get("lower")));
        });
        list.add(new PriceThresholdRule(thresholds, alertService));

        percentConfigs.forEach((sym, cfg) -> {
            int tf = (Integer) cfg.get("timeframe");
            int c  = (Integer) cfg.get("candles");
            BigDecimal pct = new BigDecimal(cfg.get("percent").toString());
            list.add(new PercentChangeRule(c, pct, tf, alertService));
        });

        return list;
    }
}
