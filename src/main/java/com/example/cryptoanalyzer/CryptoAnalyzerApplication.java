
package com.example.cryptoanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CryptoAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoAnalyzerApplication.class, args);
    }
}
