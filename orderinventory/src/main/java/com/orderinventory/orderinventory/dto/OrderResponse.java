package com.orderinventory.orderinventory.dto;

import java.time.LocalDateTime;

public class OrderResponse {

    private Long id;
    private String username;
    private String status;
    private LocalDateTime createdAt;

    public OrderResponse(Long id, String username, String status, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}