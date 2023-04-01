package com.yurim.stockmanagement.service;

import com.yurim.stockmanagement.domain.Stock;
import com.yurim.stockmanagement.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptimisticLockStockService {

    private StockRepository stockRepository;

    public OptimisticLockStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional //lock 걸때 @Transactional 없으면 에러남
    public void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findByIdOptimisticLock(id);

        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
