package com.orderinventory.orderinventory.repository;

import com.orderinventory.orderinventory.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}