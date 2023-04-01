# stock-management

[인프런강의 - 재고시스템으로 알아보는 동시성이슈 해결방법](https://inf.run/Jhu5) 을 통해 학습한 내용입니다.

</br>

## race condition이 발생할 수 있는 서비스 코드


```java

@Transactional
public void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
 }

```

위 코드가 동시에 요청될때, @Transaction이 서비스를 래핑한 클래스를 새로 생성해서 사용하면서,    
transaction 의 begin ~ end 사이에 다른 transaction에서 요청하면 **커밋되지 않은 값을 읽어와 update가 누락**될 수 있다.

</br>

## 1. Synchronized

```java

public synchronized void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

```

`synchronized`를 통해 프로세스 내 동시요청을 제어할 수 있다. 이떄 @Transactional을 붙이면 도로아미타불이된다.    
하지만 **synchronized는 하나의 프로세스에서만 보장되기 때문에 서버가 1대일때만 유효**하다. 실제 환경에서 서버가 1대일 확률은 저조하므로 다른 방법이 필요하다.

</br>

(궁금증)
@Transactional을 어느정도 레벨로 거는게 맞나??? (method? class?)   
<img width="456" alt="image" src="https://user-images.githubusercontent.com/45115557/229291389-c4ecc852-c906-4529-8c7c-8f7f20fd5879.png">
차이점이 뭔지

</br>

## 2. Database 이용

1. PassimisticLock(비관적 lock)

실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법으로, execlusive lock을 걸게 되면 다른 트랜젝션에서는 lock이 해제되기 전에 데이터를 가져갈 수 없음

### 장점

* 충돌이 자주 발생하는 서비스의 경우 optimistic lock 보다 성능이 좋을 수 있음

### 단점

* 별도의 lock을 걸기 때문에 성능 감소가 있을 수 있음
* deadlock 이 걸릴 수 있기 때문에 조심해서 사용하여야 함 

</br>

**StockRespository**

```java

public interface StockRepository extends JpaRepository<Stock,Long> {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id=:id")
    Stock findByIdWithPessimisticLock(Long id);
}
```

</br>

**PessimisticLockStockService**

```java

@Service
public class PessimisticLockStockService {

    private StockRepository stockRepository;

    public PessimisticLockStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findByIdWithPessimisticLock(id);

        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}

```

여기에서 주의할 점은 @Transactional을 꼭 붙여줘야 한다는 것인데, 비관적 lock을 걸면 select for update를 하게 되는데 @Transactional이 없으면 오류가 발생한다. 

<img width="1263" alt="image" src="https://user-images.githubusercontent.com/45115557/229291786-02bdb9d4-b572-4ab3-a8b5-484eb0c46902.png">


select for update가 잘 실행되는 것을 알 수 있고, 정상적으로 재고가 감소하게 된다. 
