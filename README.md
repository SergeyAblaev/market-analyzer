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
