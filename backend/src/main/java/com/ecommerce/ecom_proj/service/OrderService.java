package com.ecommerce.ecom_proj.service;

import com.ecommerce.ecom_proj.dto.OrderItemRequest;
import com.ecommerce.ecom_proj.dto.OrderRequest;
import com.ecommerce.ecom_proj.dto.OrderResponse;
import com.ecommerce.ecom_proj.exception.InsufficientStockException;
import com.ecommerce.ecom_proj.exception.ResourceNotFoundException;
import com.ecommerce.ecom_proj.model.Order;
import com.ecommerce.ecom_proj.model.OrderItem;
import com.ecommerce.ecom_proj.model.OrderStatus;
import com.ecommerce.ecom_proj.model.Product;
import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.OrderRepo;
import com.ecommerce.ecom_proj.repo.ProductRepo;
import com.ecommerce.ecom_proj.repo.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class OrderService {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;

    public OrderService(OrderRepo orderRepo, ProductRepo productRepo, UserRepo userRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    /**
     * Places an order atomically. The product rows are locked (SELECT ... FOR UPDATE) for the duration of the
     * transaction, so two concurrent checkouts can never both consume the same remaining stock. Any failure
     * rolls back the whole order, including the stock already decremented for earlier items.
     */
    @Transactional
    public OrderResponse placeOrder(String userEmail, OrderRequest request) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Merge duplicate lines for the same product; a sorted map keeps lock order deterministic.
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : request.items()) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        List<Product> products = productRepo.findAllByIdForUpdate(quantities.keySet());
        if (products.size() != quantities.size()) {
            Long missing = quantities.keySet().stream()
                    .filter(id -> products.stream().noneMatch(p -> p.getId().equals(id)))
                    .findFirst().orElseThrow();
            throw new ResourceNotFoundException("Product with id " + missing + " not found");
        }

        // Validate every line before changing anything, so a rejected order leaves all stock untouched.
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            if (!product.isProductAvailable()) {
                throw new InsufficientStockException("'" + product.getName() + "' is currently unavailable");
            }
            if (product.getStockQuantity() < quantity) {
                throw new InsufficientStockException("Only " + product.getStockQuantity()
                        + " unit(s) of '" + product.getName() + "' left in stock");
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            product.setStockQuantity(product.getStockQuantity() - quantity);

            OrderItem line = new OrderItem();
            line.setProduct(product);
            line.setProductName(product.getName());
            line.setUnitPrice(product.getPrice());
            line.setQuantity(quantity);
            order.addItem(line);
            total = total.add(line.getLineTotal());
        }
        order.setTotalAmount(total);

        return OrderResponse.from(orderRepo.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(String userEmail) {
        return orderRepo.findByUserEmailOrderByCreatedAtDescIdDesc(userEmail).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, String userEmail, boolean isAdmin) {
        Order order = orderRepo.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " not found"));
        // Someone else's order is reported as missing so its existence is not leaked.
        if (!isAdmin && !order.getUser().getEmail().equals(userEmail)) {
            throw new ResourceNotFoundException("Order with id " + id + " not found");
        }
        return OrderResponse.from(order);
    }
}
