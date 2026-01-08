package com.example.cryptoanalyzer.web;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.alerts.service.AlertEventService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertEventService service;

    public AlertController(AlertEventService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlertEvent> list(
            @RequestParam(name = "symbol", required = false)
            @Schema(defaultValue = "BTCUSDT") String symbol
    ) {
        if (symbol == null) {
            return service.getRecent(null);
        }
        return service.getRecent(symbol.toLowerCase());
    }
}
