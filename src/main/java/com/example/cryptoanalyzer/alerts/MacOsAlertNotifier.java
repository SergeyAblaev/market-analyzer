
package com.example.cryptoanalyzer.alerts;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("macos")
@Slf4j
public class MacOsAlertNotifier implements AlertNotifier {

    public void notify(String title, String message) {
        try {
            new ProcessBuilder(
                    "osascript",
                    "-e",
                    "display notification \"" + message + "\" with title \"" + title + "\""
            ).start();
        } catch (Exception e) {
            log.error("Error notifying user", e);
        }
    }

    @Override
    public void notify(AlertEvent event) {
        String title = event.getSymbol();
        String message = event.getMessage();
        notify(title, message);
    }
}
