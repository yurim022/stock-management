package com.yurim.stockmanagement.service;

import com.yurim.stockmanagement.StockRepository;
import com.yurim.stockmanagement.domain.Stock;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();

        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}
