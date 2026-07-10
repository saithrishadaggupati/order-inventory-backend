package com.orderinventory.orderinventory.service;

import com.orderinventory.orderinventory.entity.Order;
import com.orderinventory.orderinventory.entity.OrderItem;
import com.orderinventory.orderinventory.entity.Product;
import com.orderinventory.orderinventory.entity.User;
import com.orderinventory.orderinventory.repository.OrderItemRepository;
import com.orderinventory.orderinventory.repository.OrderRepository;
import com.orderinventory.orderinventory.repository.ProductRepository;
import com.orderinventory.orderinventory.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final com.orderinventory.orderinventory.repository.OutboxEventRepository outboxEventRepository;

    public OrderService(ProductRepository productRepository, OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository, UserRepository userRepository,
                         com.orderinventory.orderinventory.repository.OutboxEventRepository outboxEventRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    // called by the retry wrapper - each attempt gets its own fresh transaction.
    // the pessimistic lock inside findByIdForUpdate is what actually stops overselling here.
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "products", key = "#productId")
    public Order placeOrderAttempt(String username, Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Not enough stock for product " + productId);
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Order order = new Order();
        order.setUser(user);
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setPrice(product.getPrice());
        orderItemRepository.save(item);

        // written in the SAME transaction as the order - this is the whole point of
        // the outbox pattern. if the order commits, this event is guaranteed to exist
        // too. no separate call to a message broker here that could fail independently.
        com.orderinventory.orderinventory.entity.OutboxEvent event = new com.orderinventory.orderinventory.entity.OutboxEvent();
        event.setEventType("ORDER_PLACED");
        event.setPayload("Order " + order.getId() + " placed by " + username + " for product " + productId + ", qty " + quantity);
        outboxEventRepository.save(event);

        // low stock notification - fires a second event if stock is running low after this order
        if (product.getStock() < 3) {
            com.orderinventory.orderinventory.entity.OutboxEvent lowStockEvent = new com.orderinventory.orderinventory.entity.OutboxEvent();
            lowStockEvent.setEventType("LOW_STOCK");
            lowStockEvent.setPayload("Product " + productId + " is low on stock: " + product.getStock() + " remaining");
            outboxEventRepository.save(lowStockEvent);
        }

        return order;
    }
}