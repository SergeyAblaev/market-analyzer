package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.AlertNotifier;
import com.example.cryptoanalyzer.alerts.service.AlertEventService;
import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

//    private final List<AlertNotifier> notifiers;
//    private final RuleEngine engine;
    private final List<AlertRule> rules;
    private final AlertEventService alertService;
    private final Set<AlertNotifier> notifiers; // Is a set of possible notifiers (1 at this moment)


//    public RuleEngineService(List<AlertRule> rules, AlertEventService alertService) {
//        this.rules = rules;
//        this.alertService = alertService;
//    }

    public void process(OhlcCandle candle) {
        log.info("Processing candle {} {}", candle.getSymbol(), candle.getClosePrice());

        for (AlertRule rule : rules) {
            rule.evaluate(candle).ifPresent(event -> {
                alertService.save(event);
                notifiers.forEach(n -> n.notify(event));
            });
        }

 //       for (AlertRule rule : rules) {
 //           Optional<AlertEvent> maybe = rule.evaluate(candle);
 //           maybe.ifPresent(alertService::save);
 //       }
    }

//    public void process(OhlcCandle candle) {
//        List<AlertEvent> events = engine.evaluate(candle);
//
//        for (AlertEvent event : events) {
//            alertService.save(event);
//            notifiers.forEach(n -> n.notify(event));
//        }
//    }

}
