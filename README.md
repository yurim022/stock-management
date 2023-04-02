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

# 1. Application Level - Synchronized

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

</br></br>

# 2. Database

적용에 앞서 shared lock과 exclusive 의 차이점을 다시 복습해보자. 

![image](https://user-images.githubusercontent.com/45115557/229293553-7507a796-2d62-437f-9b98-7686cea52fba.png)

![image](https://user-images.githubusercontent.com/45115557/229293599-b7030f83-05a1-441d-b6f2-8596bfbbb469.png)

출처: https://www.geeksforgeeks.org/difference-between-shared-lock-and-exclusive-lock/


</br></br>

## PassimisticLock(비관적 lock)

실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법으로, execlusive lock을 걸게 되면 다른 트랜젝션에서는 lock이 해제되기 전에 데이터를 가져갈 수 없음


### Lock Modes
* `PASSIMISTIC_READ` : shared lock(s-lock) 으로 update, delete 가 불가능하다.
* `PESSIMISTIC_WRITE` : exclusive lock(x-lock) 으로 lock을 획득하지 못한 트랜잭션이 read,update,delete 를 할 수 없다.
* `PESSIMISTIC_FORCE_INCREMENT`: `PESSIMISTIC_WRITE`와 동일하게 동작하며 추가로 엔티티 버전을 업데이트 한다. 
   

### 장점

* 충돌이 자주 발생하는 서비스의 경우 optimistic lock 보다 성능이 좋을 수 있음
* 강력한 데이터 정합성을 보장함

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

여기에서 주의할 점은 @Transactional을 꼭 붙여줘야 한다는 것인데, 비관적 lock을 걸면 select for update를 하는데 @Transactional이 없으면 의미가 없어서인지...오류가 발생한다. (이유 아는사람은 가르쳐주세요...) 

<img width="1263" alt="image" src="https://user-images.githubusercontent.com/45115557/229291786-02bdb9d4-b572-4ab3-a8b5-484eb0c46902.png">


select for update가 잘 실행되는 것을 알 수 있고, 정상적으로 재고가 감소하게 된다. 

</br></br>


## Optimistic lock(낙관적)

실제로 Lock을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법.    
먼저 데이터를 읽은 후에 update를 수행할 때 현재 내가 읽은 버전에 맞는지 확인하여 업데이트하며, 수정사항이 있을때 (버전이 다를 때) 다시 읽은 후 작업을 수행함.

### 장점

* 충돌이 많지 않은 경우엔 pessimistic보다 빠름

### 단점

* 버전이 맞지 않을 때 재시도 로직을 개발자가 구현해주어야 함
* 충돌이 많을 경우 느림

```java

public interface StockRepository extends JpaRepository<Stock,Long> {

    @Lock(value = LockModeType.OPTIMISTIC)
    @Query("select s from Stock s where s.id=:id")
    Stock findByIdOptimisticLock(Long id);
}

```

optimisticlock 코드를 레포지토리에 추가해준다.

```java

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


```

update 시 버전이 맞지 않을  재시도 하는 로직을 구현해준다.


```java

@Service
public class OptimisticLockStockFacade {

    private OptimisticLockStockService optimisticLockStockService;

    public OptimisticLockStockFacade(OptimisticLockStockService optimisticLockStockService) {
        this.optimisticLockStockService = optimisticLockStockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {

        //실패시 재요청 할 수 있도록 함
        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);

                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
    }
}

```

<img width="1284" alt="스크린샷 2023-04-01 오후 11 19 27" src="https://user-images.githubusercontent.com/45115557/229296360-02859132-c7a3-4e06-83d2-b77a7d31d41d.png">


테스트를 돌려보면, 버전을 조회하는걸 확인할 수 있다. 



</br></br>

## Named lock 

이름을 가진 metadata lock이다. 이름을 가진 lock을 획득한 후 해제할때까지 다른 세션은 이 lock을 획득할 수 없다. 

### 장점

* 분산환경에서 사용할 수 있다. (여러대의 서버)
* timeout으로 인한 lock 해지 구현이 쉽다.

### 단점

* 트랜젝션이 종료될 때 lock이 자동으로 해제되지 않기 때문에 별도의 명령어로 해제를 수행하거나 선점시간이 끝나야 해제된다.
* 커넥션을 잡아먹기 때문에 상용에서는 별도의 데이터소스를 사용해야 한다.


NativeQuery를 사용하기 위해 별도의 lockRepository를 구현한다. 

```java

public interface LockRepository extends JpaRepository<Stock,Long> {
    //실무에서는 별도의 jdbc를 사용하는 등 datasource 새로 정의해서 사용하기 (connection 부족할 수 있음)
    //named lock은 분산 lock 구현할때 주로 사용. pessimistic에 비해 timed out 을 사용하기 좋음
    //transactional 종료 시 세션관리와 lock 해제를 잘 해주어야 하므로 주의해야 함
    //실제 사용시 구현이 복잡할 수 있음

    @Query(value = "select get_lock(:key, 3000)",nativeQuery = true)
    void getLock(String key);

    @Query(value = "select release_lock(:key)",nativeQuery = true)
    void releaseLock(String key);
}


```

NamedLock을 사용할 때에는 @Transactional에서 Propagation.REQUIRES_NEW 를 해주어야 한다. 

```java

@Service
public class StockService {

    private StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // for named lock
    public synchronized void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}

```

lock을 getLock 하고 release 하는 코드를 작성한다. 

```java

@Component
public class NamedLockStockFacade {

    private final LockRepository lockRepository;

    private final StockService stockService;

    public NamedLockStockFacade(LockRepository lockRepository, StockService stockService) {
        this.lockRepository = lockRepository;
        this.stockService = stockService;
    }

    @Transactional
    public void decrease(Long id, Long quantity) {

        try {
            lockRepository.getLock(id.toString());
            stockService.decrease(id,quantity);
        } finally {
            lockRepository.releaseLock(id.toString());
        }
    }

}

```

<img width="1187" alt="image" src="https://user-images.githubusercontent.com/45115557/229296886-6e3a1859-a074-49b0-97d1-5b57a3f7e809.png">
 
 테스트를 돌려보면 lock을 획득하고 업데이트하고 해제하는걸 볼 수 있다. 

</br></br>

# 3. Redis

## Redission

NamdedLock과 비슷하게 동작하며, setnx를 통해 키가 없으면 세팅하고 없으면 접근할 수 없다. spinlock 방식으로 키를 획득할때까지 재시도 로직을 개발자가 구현해주어야 한다.   
NamedLock과 다른 점은 세션관리를 해줄 필요가 없다는 것이다.   


</br>

참고링크:    
https://www.baeldung.com/jpa-pessimistic-locking   
https://www.geeksforgeeks.org/difference-between-shared-lock-and-exclusive-lock/   



