package com.example.cryptoanalyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "alerts")
@Getter @Setter
public class AlertProperties {
    // Fields are mapped from YAML (price-thresholds -> priceThresholds)
    private Map<String, Threshold> priceThresholds;
    private Map<String, PercentChangeConfig> percentChange;
    private Map<String, ImpulseMoveConfig> impulseMove;

    @Getter @Setter
    public static class Threshold {
        private BigDecimal upper;
        private BigDecimal lower;
    }

    @Getter @Setter
    public static class PercentChangeConfig {
        private Integer timeframe;
        private Integer candles;
        private Double percent;
    }

    @Getter @Setter
    public static class ImpulseMoveConfig {
        private Integer timeframe;
        private Double directionRatio;
        private Double minTotalMovePercent;
        private Double accelerationFactor;
    }
}

