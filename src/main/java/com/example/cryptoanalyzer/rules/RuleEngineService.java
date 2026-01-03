package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.service.AlertEventService;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RuleEngineService {

    private final List<AlertRule> rules;
    private final AlertEventService alertService;

    public RuleEngineService(List<AlertRule> rules, AlertEventService alertService) {
        this.rules = rules;
        this.alertService = alertService;
    }

    public void process(OhlcCandle candle) {
        for (AlertRule rule : rules) {
            Optional<AlertEvent> maybe = rule.evaluate(candle);
            maybe.ifPresent(alertService::save);
        }
    }
}
