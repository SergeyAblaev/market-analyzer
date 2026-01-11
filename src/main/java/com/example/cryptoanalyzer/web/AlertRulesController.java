package com.example.cryptoanalyzer.web;

import com.example.cryptoanalyzer.rules.AlertRule;
import com.example.cryptoanalyzer.rules.service.AlertRulesService;
import com.example.cryptoanalyzer.web.model.AlertRuleUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alertrules")
public class AlertRulesController {

    private final AlertRulesService service;


    @GetMapping("/all")
    public List<AlertRule> getRules() {
        return service.getRules();
    }

    @GetMapping("/update")
    public AlertRule update(AlertRuleUpdateDto dto) {
        return service.update(dto);
    }
}
