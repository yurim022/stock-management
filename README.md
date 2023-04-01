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

## 2. Database 이용

1. Optimi
