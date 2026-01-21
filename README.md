## Market-analyzer links
* http://localhost:8080/ — BTCUSDT chart
* http://localhost:8080/actuator
* http://localhost:8080/h2-console
* http://localhost:8080/swagger-ui/index.html

It's an Event-Driven Architecture (EDA), with Streaming / Reactive pipeline.

## Setup instructions:

Todo - Create instructions!

## Data Flow:

Binance WS  
↓  
TradeEvent  
↓  
OHLC Aggregator  
↓  
Closed Candle (here is calling RuleEngine)  
↓  
RuleEngine   (stateful rules with candle history)
↓  
MacOS Alert  

## Rules:

PriceThresholdRule
PercentChangeRule - Checks that the price has changed by Y% in X minutes
ImpulseMoveRule - Calculate ALERT: STRONG BULLISH IMPULSE / BEARISH IMPULSE

## Todo:

Now needs to fix:
1. PercentChangeRule.java !!! Check it.
