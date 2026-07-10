package com.orderinventory.orderinventory;

import com.orderinventory.orderinventory.entity.Product;
import com.orderinventory.orderinventory.entity.User;
import com.orderinventory.orderinventory.repository.ProductRepository;
import com.orderinventory.orderinventory.repository.UserRepository;
import com.orderinventory.orderinventory.service.OrderPlacementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
public class OrderConcurrencyTest {

    // spins up a real, throwaway postgres instance just for this test run -
    // not the same DB you use for manual testing, so nothing here touches your real data
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderPlacementService orderPlacementService;

    private Long productId;

    @BeforeEach
    void setUp() {
        // fresh low-stock product + a test user before every run
        Product product = new Product();
        product.setName("Concurrency Test Widget");
        product.setPrice(new BigDecimal("9.99"));
        product.setStock(10);
        product = productRepository.save(product);
        productId = product.getId();

        if (userRepository.findByUsername("concurrencytester").isEmpty()) {
            User user = new User();
            user.setUsername("concurrencytester");
            user.setPassword(new BCryptPasswordEncoder().encode("dummy"));
            user.setRole("USER");
            userRepository.save(user);
        }
    }

    @Test
    void fiftyConcurrentOrdersOnTenStock_shouldNeverOversell() throws InterruptedException {
        int totalRequests = 50;
        int stockAvailable = 10;

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    orderPlacementService.placeOrder("concurrencytester", productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Product finalProduct = productRepository.findById(productId).orElseThrow();

        System.out.println("Successful orders: " + successCount.get());
        System.out.println("Failed orders: " + failCount.get());
        System.out.println("Final stock: " + finalProduct.getStock());

        // the core assertion - stock must never go negative, no matter how many
        // requests raced for it
        assertTrue(finalProduct.getStock() >= 0, "Stock went negative - overselling occurred!");

        // successful orders should exactly match the stock consumed
        assertEquals(stockAvailable - finalProduct.getStock(), successCount.get(),
                "Successful order count doesn't match stock consumed");

        // total requests should equal success + fail, nothing silently lost
        assertEquals(totalRequests, successCount.get() + failCount.get());
    }
}