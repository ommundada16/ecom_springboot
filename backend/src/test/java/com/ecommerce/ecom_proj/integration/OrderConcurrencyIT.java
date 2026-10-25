package com.ecommerce.ecom_proj.integration;

import com.ecommerce.ecom_proj.dto.OrderItemRequest;
import com.ecommerce.ecom_proj.dto.OrderRequest;
import com.ecommerce.ecom_proj.exception.InsufficientStockException;
import com.ecommerce.ecom_proj.model.Product;
import com.ecommerce.ecom_proj.model.Role;
import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.OrderRepo;
import com.ecommerce.ecom_proj.repo.ProductRepo;
import com.ecommerce.ecom_proj.repo.UserRepo;
import com.ecommerce.ecom_proj.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional on purpose: each checkout must run in its own real, committed transaction
 * for the row locking to be exercised.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyIT {

    private static final int INITIAL_STOCK = 5;
    private static final int CONCURRENT_BUYERS = 8;

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private OrderRepo orderRepo;

    private Product product;
    private final List<User> users = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        users.forEach(u -> orderRepo.deleteAll(orderRepo.findByUserEmailOrderByCreatedAtDescIdDesc(u.getEmail())));
        userRepo.deleteAll(users);
        if (product != null) {
            productRepo.deleteById(product.getId());
        }
    }

    private User newUser(int index) {
        User user = userRepo.save(User.builder().name("Buyer " + index)
                .email("buyer-" + UUID.randomUUID() + "@shop.com").password("x").role(Role.USER)
                .createdAt(LocalDateTime.now()).build());
        users.add(user);
        return user;
    }

    @Test
    void rejectedMultiItemOrder_leavesEveryProductsCommittedStockUntouched() {
        product = productRepo.save(Product.builder().name("Plentiful").price(new BigDecimal("10.00"))
                .productAvailable(true).stockQuantity(20).build());
        Product scarce = productRepo.save(Product.builder().name("Scarce").price(new BigDecimal("10.00"))
                .productAvailable(true).stockQuantity(1).build());
        User buyer = newUser(0);

        try {
            org.junit.jupiter.api.Assertions.assertThrows(InsufficientStockException.class,
                    () -> orderService.placeOrder(buyer.getEmail(), new OrderRequest(List.of(
                            new OrderItemRequest(product.getId(), 5), new OrderItemRequest(scarce.getId(), 2)))));

            assertThat(productRepo.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(20);
            assertThat(productRepo.findById(scarce.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
            assertThat(orderRepo.findByUserEmailOrderByCreatedAtDescIdDesc(buyer.getEmail())).isEmpty();
        } finally {
            productRepo.deleteById(scarce.getId());
        }
    }

    @Test
    void concurrentCheckouts_neverOversellTheStock() throws Exception {
        product = productRepo.save(Product.builder().name("Hot Item").price(new BigDecimal("10.00"))
                .productAvailable(true).stockQuantity(INITIAL_STOCK).build());
        for (int i = 0; i < CONCURRENT_BUYERS; i++) {
            newUser(i);
        }

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_BUYERS);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (User buyer : users) {
            Callable<Boolean> checkout = () -> {
                startGun.await();
                try {
                    orderService.placeOrder(buyer.getEmail(),
                            new OrderRequest(List.of(new OrderItemRequest(product.getId(), 1))));
                    return true;
                } catch (InsufficientStockException e) {
                    return false;
                }
            };
            results.add(pool.submit(checkout));
        }
        startGun.countDown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get(30, TimeUnit.SECONDS)) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(INITIAL_STOCK);
        assertThat(productRepo.findById(product.getId()).orElseThrow().getStockQuantity()).isZero();
        long ordersPlaced = users.stream()
                .mapToLong(u -> orderRepo.findByUserEmailOrderByCreatedAtDescIdDesc(u.getEmail()).size())
                .sum();
        assertThat(ordersPlaced).isEqualTo(INITIAL_STOCK);
    }
}
