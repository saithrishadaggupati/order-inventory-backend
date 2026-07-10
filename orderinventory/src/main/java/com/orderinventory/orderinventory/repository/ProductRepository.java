package com.orderinventory.orderinventory.repository;

import com.orderinventory.orderinventory.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // pessimistic write lock - this is what actually prevents overselling under concurrent requests.
    // the row stays locked until the transaction commits/rolls back, so a second request
    // trying to grab the same product has to wait its turn instead of racing.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}