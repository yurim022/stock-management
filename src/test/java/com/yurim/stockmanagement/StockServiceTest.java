package com.yurim.stockmanagement;

import com.yurim.stockmanagement.domain.Stock;
import com.yurim.stockmanagement.repository.StockRepository;
import com.yurim.stockmanagement.service.PessimisticLockStockService;
import com.yurim.stockmanagement.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class StockServiceTest {

//    private StockService stockService;
    @Autowired
    private PessimisticLockStockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before(){
        Stock stock = new Stock(1L, 100L);

        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    @Test
    public void stock_decrease(){
        stockService.decrease(1L, 1L);

        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(99,stock.getQuantity());
    }

    @Test
    public void 동시에_100개의_요청() throws InterruptedException {

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32); //비동기 서비스 실행 단순화하여 제공
        CountDownLatch latch = new CountDownLatch(threadCount); // 100개의 요청이 끝날때까지 기다림

        for(int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try{
                stockService.decrease(1L, 1L);
                }finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(0L,stock.getQuantity());
        // 1. synchronized 사용
        // 함수 앞에 @Transactional + synchronized 붙여도 fail -> commit 되기 이전의 값을 read 할 수 있음
        // @Transactional 을 제거하고 synchronized 만 하면 성공
        // 하지만 synchronized 는 하나의 프로세스 안에서만 보장됨. 서버가 1대일때는 괜찮지만 서버가 2대 이상일때는 데이터 접근을 여러대에서 할 수 있어서 race condition 발생 가능


    }
}
