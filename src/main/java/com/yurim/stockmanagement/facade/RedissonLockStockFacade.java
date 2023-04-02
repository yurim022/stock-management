package com.yurim.stockmanagement.facade;

import com.yurim.stockmanagement.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedissonLockStockFacade {

    private RedissonClient redissonClient; //별도 repository 생성x
    private StockService stockService;

    public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    public void decrease(Long key, Long quantity) {
        RLock lock = redissonClient.getLock(key.toString());
        
        try {
            boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS); //5초 기다리고 1초 점유

            if (!available) {
                System.out.println("lock 획득 실패"); //실제환경에선 log로!
                return;
            }

            stockService.decrease(key,quantity);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
