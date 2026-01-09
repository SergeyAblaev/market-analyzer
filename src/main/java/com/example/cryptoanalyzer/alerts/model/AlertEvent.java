package com.example.cryptoanalyzer.alerts.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "alert_events")
@Getter
@Setter
@NoArgsConstructor
public class AlertEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private int timeframe;
    private String ruleType;
    private String message;
    private Instant triggeredAt;
    private AlertDirection direction;

    public AlertEvent(String symbol, int timeframe, String ruleType, String message, AlertDirection direction) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.ruleType = ruleType;
        this.message = message;
        this.triggeredAt = Instant.now();
        this.direction = direction;
    }

}
