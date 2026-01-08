package com.example.cryptoanalyzer.ohlc.service;

import com.example.cryptoanalyzer.ohlc.model.OhlcCandle;
import com.example.cryptoanalyzer.ohlc.repository.OhlcCandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class OhlcCandleService {

    private final OhlcCandleRepository repository;

    public Collection<OhlcCandle> findAllBySymbol(String symbol) {
      return repository.findAllBySymbol(symbol);
    }

    public Page<OhlcCandle> getPage(Pageable pageable) {
      return repository.findAll(pageable);
    }
}
