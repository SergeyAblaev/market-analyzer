
package com.example.cryptoanalyzer.alert;

import org.springframework.stereotype.Service;

@Service
public class MacOsAlertService {

    public void notify(String title, String message) {
        try {
            new ProcessBuilder(
                "osascript",
                "-e",
                "display notification \"" + message + "\" with title \"" + title + "\""
            ).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
