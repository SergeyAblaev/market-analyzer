package com.example.cryptoanalyzer.ohlc.repository;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OhlcCandleRepository extends JpaRepository<OhlcCandle, Long> {
}