# market-analyzer

Data Flow:

Binance WS
↓
TradeEvent
↓
OhlcAggregator
↓
Closed Candle (ready for DB / Rules / UI)
