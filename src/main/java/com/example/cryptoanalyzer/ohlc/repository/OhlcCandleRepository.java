package com.example.cryptoanalyzer.ohlc.repository;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

@Repository
public interface OhlcCandleRepository extends JpaRepository<OhlcCandle, Long> {
    Page<OhlcCandle> findAll(Pageable pageable);

    Collection<OhlcCandle> findAllBySymbol(String symbol);
}