package com.example.cryptoanalyzer.alerts.service;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import com.example.cryptoanalyzer.alerts.repository.AlertEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertEventService {

    private final AlertEventRepository repo;

    public AlertEventService(AlertEventRepository repo) {
        this.repo = repo;
    }

    public AlertEvent save(AlertEvent event) {
        return repo.save(event);
    }

    public List<AlertEvent> getRecent(String symbol) {
        return repo.findAllBySymbol(symbol.toUpperCase());
    }

    public List<AlertEvent> getAll() {
        return repo.findAll();
    }
}
