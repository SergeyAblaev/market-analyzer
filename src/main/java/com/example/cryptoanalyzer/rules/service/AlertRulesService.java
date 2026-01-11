package com.example.cryptoanalyzer.rules.service;

import com.example.cryptoanalyzer.rules.AlertRule;
import com.example.cryptoanalyzer.rules.PercentChangeRule;
import com.example.cryptoanalyzer.web.model.AlertRuleUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertRulesService {
    private final List<AlertRule> rules;

    public List<AlertRule> getRules() {
        return rules;
    }

    public AlertRule update(AlertRuleUpdateDto dto) {

        AlertRule rule = rules.stream()
                .filter(r -> r.getId().equals(dto.id()))
                .findFirst()
                .orElseThrow();

        rule.updateFrom(dto);

//        if (rule instanceof PercentChangeRule p) {
//        }

        return rule;
    }

}
