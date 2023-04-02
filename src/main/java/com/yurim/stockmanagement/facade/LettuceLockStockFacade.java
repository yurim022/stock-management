package com.yurim.stockmanagement.facade;

import com.yurim.stockmanagement.repository.RedisLockRepository;
import com.yurim.stockmanagement.service.StockService;
import org.springframework.stereotype.Component;

@Component
public class LettuceLockStockFacade {

    private RedisLockRepository redisLockRepository;

    private StockService stockService;

    public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) throws InterruptedException {
        while (!redisLockRepository.lock(key)){
            Thread.sleep(1100);
        }

        try {
            stockService.decrease(key,quantity);
        } finally {
            redisLockRepository.unlock(key);
        }
    }
}
