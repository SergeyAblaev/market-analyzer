# market-analyzer links
http://localhost:8080/
http://localhost:8080/actuator
http://localhost:8080/h2-console
http://localhost:8080/swagger-ui/index.html

It's an Event-Driven Architecture (EDA), witch Streaming / Reactive pipeline

# Data Flow :

Binance WS
↓
TradeEvent
↓
OHLC Aggregator
↓
Closed Candle  (here is calling RuleEngine)
↓
RuleEngine   
↓
MacOS Alert

# Install

CREATE USER marketanalyse WITH PASSWORD 'str0ng...';
CREATE DATABASE marketanalyse OWNER marketanalyse;

# Todo:

Now needs to fix:
1 PercentChangeRule.java !!
