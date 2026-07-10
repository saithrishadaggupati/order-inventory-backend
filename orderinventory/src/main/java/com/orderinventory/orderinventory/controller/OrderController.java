package com.orderinventory.orderinventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderinventory.orderinventory.dto.OrderRequest;
import com.orderinventory.orderinventory.dto.OrderResponse;
import com.orderinventory.orderinventory.entity.IdempotencyKey;
import com.orderinventory.orderinventory.entity.Order;
import com.orderinventory.orderinventory.service.IdempotencyService;
import com.orderinventory.orderinventory.service.OrderPlacementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderPlacementService orderPlacementService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public OrderController(OrderPlacementService orderPlacementService, IdempotencyService idempotencyService,
                            ObjectMapper objectMapper) {
        this.orderPlacementService = orderPlacementService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                         Authentication authentication) throws Exception {

        String username = authentication.getName();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return doPlaceOrder(username, request);
        }

        boolean claimed = idempotencyService.tryClaimKey(idempotencyKey);

        if (!claimed) {
            IdempotencyKey existing = idempotencyService.getExisting(idempotencyKey)
                    .orElseThrow(() -> new RuntimeException("Idempotency key claimed but record missing - race condition edge case"));

            if (existing.getResponseBody() == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Request with this idempotency key is still processing, try again shortly");
            }

            // return the exact JSON we stored last time, as-is - not re-parsed
            return ResponseEntity.status(existing.getResponseStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(existing.getResponseBody());
        }

        ResponseEntity<?> response = doPlaceOrder(username, request);

        // serialize the actual response body to real JSON before storing it,
        // so a replayed request gets back valid JSON, not a java toString()
        String jsonBody = objectMapper.writeValueAsString(response.getBody());
        idempotencyService.saveResponse(idempotencyKey, response.getStatusCodeValue(), jsonBody);

        return response;
    }

    private ResponseEntity<?> doPlaceOrder(String username, OrderRequest request) {
        try {
            Order order = orderPlacementService.placeOrder(username, request.getProductId(), request.getQuantity());
            OrderResponse response = new OrderResponse(
                    order.getId(),
                    order.getUser().getUsername(),
                    order.getStatus(),
                    order.getCreatedAt()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}