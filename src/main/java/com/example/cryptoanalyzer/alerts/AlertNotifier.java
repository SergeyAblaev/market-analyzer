package com.example.cryptoanalyzer.alerts;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;

public interface AlertNotifier {
    void notify(AlertEvent event);
}
