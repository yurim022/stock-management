package com.yurim.stockmanagement;

import com.yurim.stockmanagement.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock,Long> {
}
