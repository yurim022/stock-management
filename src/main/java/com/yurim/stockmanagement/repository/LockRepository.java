package com.yurim.stockmanagement.repository;

import com.yurim.stockmanagement.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
