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
    public String addTicker(
            @RequestParam("symbol") String symbol,
            @RequestParam("type")
            @Schema(defaultValue = "SPOT", description = "Market types 'FUTURES' or 'SPOT'")
            String typeStr
    ) {
        MarketType type;
        try {
            type = MarketType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid market type: " + typeStr);
        }
        return client.addTicker(symbol, type);
    }

    @PostMapping("/remove")
    public String removeTicker(
            @RequestParam("symbol") String symbol,
            @RequestParam("type")
            @Schema(defaultValue = "SPOT", description = "Market types 'FUTURES' or 'SPOT'")
            String typeStr
    ) {
        MarketType type;
        try {
            type = MarketType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid market type: " + typeStr);
        }
        return client.removeTicker(symbol, type);
    }

}