package com.example.cryptoanalyzer.rules;

import com.example.cryptoanalyzer.alerts.AlertNotifier;
import com.example.cryptoanalyzer.alerts.service.AlertEventService;
import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final List<AlertRule> rules;
    private final AlertEventService alertService;
    private final Set<AlertNotifier> notifiers; // Is a set of possible notifiers (1 at this moment)

    public void process(OhlcCandle candle) {
        log.info("Processing candle {} {}", candle.getSymbol(), candle.getClosePrice());

        for (AlertRule rule : rules) {
            rule.evaluate(candle).ifPresent(event -> {
                alertService.save(event);
                notifiers.forEach(n -> n.notify(event));
            });
        }
    }
}
