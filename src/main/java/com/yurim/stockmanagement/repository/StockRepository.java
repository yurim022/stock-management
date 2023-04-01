package com.yurim.stockmanagement.repository;

import com.yurim.stockmanagement.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock,Long> {
}
