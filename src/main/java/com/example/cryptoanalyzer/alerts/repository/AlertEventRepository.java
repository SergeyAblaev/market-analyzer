package com.example.cryptoanalyzer.alerts.repository;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {
    List<AlertEvent> findTop100BySymbolOrderByTriggeredAtDesc(String symbol);
    List<AlertEvent> findAllBySymbol(String symbol);
}
