package com.orderinventory.orderinventory.repository;

import com.orderinventory.orderinventory.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}