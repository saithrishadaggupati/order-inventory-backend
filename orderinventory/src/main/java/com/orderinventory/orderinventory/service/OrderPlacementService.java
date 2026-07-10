package com.orderinventory.orderinventory.service;

import com.orderinventory.orderinventory.entity.Order;
import org.springframework.stereotype.Service;

@Service
public class OrderPlacementService {

    private static final int MAX_ATTEMPTS = 3;

    private final OrderService orderService;

    public OrderPlacementService(OrderService orderService) {
        this.orderService = orderService;
    }

    public Order placeOrder(String username, Long productId, int quantity) {
        int attempt = 0;
        RuntimeException lastError = null;

        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try {
                // each call here is its own fresh transaction, thanks to the proxy
                // on OrderService - that's why this class calls orderService instead
                // of just looping inside placeOrderAttempt itself
                return orderService.placeOrderAttempt(username, productId, quantity);
            } catch (org.springframework.dao.PessimisticLockingFailureException e) {
                lastError = e;
                try {
                    Thread.sleep(100L * attempt); // simple backoff: 100ms, 200ms, 300ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            // note: IllegalStateException (out of stock) is NOT caught here - it's a real
            // business failure, not a concurrency conflict, so it should fail immediately,
            // not retry
        }

        throw new RuntimeException("Order failed after " + MAX_ATTEMPTS + " attempts due to lock contention", lastError);
    }
}