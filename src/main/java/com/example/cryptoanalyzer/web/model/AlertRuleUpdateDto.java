package com.example.cryptoanalyzer.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record AlertRuleUpdateDto(
        @Schema(required = true, defaultValue = "PERCENT_CHANGE_ETHUSDT")
        String id,
        @Schema(defaultValue = "3")
        Integer candles,
        @Schema(defaultValue = "0.1")
        BigDecimal percent,

        // For ImpulseMoveRule paramethers:
        @Schema(defaultValue = "0.75")
        BigDecimal directionRatio,
        @Schema(defaultValue = "0.8")
        BigDecimal minTotalMovePercent,
        @Schema(defaultValue = "1.5")
        BigDecimal accelerationFactor

//        Map<String, PriceThresholdRule.Threshold> thresholds;

) {}
