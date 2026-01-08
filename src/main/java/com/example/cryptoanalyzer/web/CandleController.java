package com.example.cryptoanalyzer.web;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.ohlc.service.OhlcCandleService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/candle")
@RequiredArgsConstructor
public class CandleController {

    private final OhlcCandleService service;

    @GetMapping("/getAll")
    public Collection<OhlcCandle> getAll(
            @RequestParam(name = "symbol", required = false)
            @Schema(defaultValue = "BTCUSDT") String symbol
    ) {
        return service.findAllBySymbol(symbol);
    }

    @GetMapping("/getPage")
    public Page<OhlcCandle> getPage(@ParameterObject Pageable pageable) {
        return service.getPage(pageable);
    }
}
