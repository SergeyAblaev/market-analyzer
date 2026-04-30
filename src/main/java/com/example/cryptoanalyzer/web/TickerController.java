package com.example.cryptoanalyzer.web;

import com.example.cryptoanalyzer.market.ws.BinanceWebSocketClient;
import com.example.cryptoanalyzer.market.ws.MarketType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickers")
public class TickerController {

    private final BinanceWebSocketClient client;

    public TickerController(BinanceWebSocketClient client) {
        this.client = client;
    }

    @PostMapping("/add")
    public void addTicker(
            @RequestParam String symbol,
            @RequestParam
            @Schema(defaultValue = "SPOT", description = "Market types 'FUTURES' or 'SPOT'")
            MarketType type
    ) {
        client.addTicker(symbol, type);
    }
}