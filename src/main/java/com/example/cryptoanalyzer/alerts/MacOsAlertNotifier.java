
package com.example.cryptoanalyzer.alerts;

import com.example.cryptoanalyzer.alerts.model.AlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("macos")
@Slf4j
public class MacOsAlertNotifier implements AlertNotifier {

    @Override
    public void notify(AlertEvent event) {
        String emoji = emoji(event);
        String sound = sound(event);

        String title = emoji + " " + event.getSymbol();
        String message = event.getMessage();

        try {
            new ProcessBuilder(
                "osascript",
                "-e",
                String.format(
                    "display notification \"%s\" with title \"%s\" sound name \"%s\"",
                    escape(message),
                    escape(title),
                    sound
                )
            ).start();
        } catch (Exception e) {
            log.error("Error notifying user", e);
        }
    }

    private String emoji(AlertEvent e) {
        int vol = Math.abs(e.getVol());
        if (vol > 8) vol = 8;
        return switch (e.getDirection()) {
            case UP -> "⬆️" + "🟩".repeat(vol);
            case DOWN -> "⬇️" + "🟥".repeat(vol);
            default -> "🟨";
        };
    }

    private String sound(AlertEvent e) {
        return switch (e.getDirection()) {
            case UP -> "Glass";
            case DOWN -> "Basso";
            default -> "Ping";
        };
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
