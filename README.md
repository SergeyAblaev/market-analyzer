# market-analyzer links
http://localhost:8080/
http://localhost:8080/actuator
http://localhost:8080/h2-console
http://localhost:8080/swagger-ui/index.html


# Data Flow:

Binance WS
↓
TradeEvent
↓
OHLC Aggregator
↓
Closed Candle  (ВОПРОС: а надо ли писать закрытую свечу в БД вообще? может оставить только в памяти?)
↓
RuleEngine     (Проанализируй - хватит ли для работы правил движка - Мапы со свечами в памяти? или надо все же делать запросами к БД? )
↓
MacOS Alert

# Install

CREATE USER marketanalyse WITH PASSWORD 'str0ng...';
CREATE DATABASE marketanalyse OWNER marketanalyse;