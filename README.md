# market-analyzer

Data Flow:

Binance WS
↓
TradeEvent
↓
OHLC Aggregator
↓
Closed Candle
↓
RuleEngine
↓
MacOS Alert

# Install

CREATE USER marketanalyse WITH PASSWORD 'str0ng...';
CREATE DATABASE marketanalyse OWNER marketanalyse;