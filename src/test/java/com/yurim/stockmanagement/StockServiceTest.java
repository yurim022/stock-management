package com.yurim.stockmanagement;

import com.yurim.stockmanagement.domain.Stock;
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

    @Autowired
    private StockService stockService;

    @Autowired
    private  StockRepository stockRepository;

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
        //해당 테스트 fail. 수량이 감소되기 전 데이터를 읽었기 때문!!
    }
}
