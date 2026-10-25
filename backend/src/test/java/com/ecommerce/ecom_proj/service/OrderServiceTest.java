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
import com.ecommerce.ecom_proj.model.Role;
import com.ecommerce.ecom_proj.model.User;
import com.ecommerce.ecom_proj.repo.OrderRepo;
import com.ecommerce.ecom_proj.repo.ProductRepo;
import com.ecommerce.ecom_proj.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EMAIL = "buyer@shop.com";

    @Mock
    private OrderRepo orderRepo;
    @Mock
    private ProductRepo productRepo;
    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private OrderService service;

    private User buyer;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).name("Buyer").email(EMAIL).role(Role.USER).build();
    }

    private static Product product(long id, String name, String price, int stock, boolean available) {
        return Product.builder().id(id).name(name).price(new BigDecimal(price))
                .productAvailable(available).stockQuantity(stock).build();
    }

    private static OrderRequest order(OrderItemRequest... items) {
        return new OrderRequest(List.of(items));
    }

    private void stubUser() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(buyer));
    }

    private void stubSave() {
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });
    }

    @Test
    void placeOrder_decrementsStockAndCalculatesTotal() {
        Product phone = product(1, "Phone", "500.00", 5, true);
        Product case_ = product(2, "Case", "20.50", 10, true);
        stubUser();
        stubSave();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(phone, case_));

        OrderResponse response = service.placeOrder(EMAIL,
                order(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 3)));

        assertThat(phone.getStockQuantity()).isEqualTo(3);
        assertThat(case_.getStockQuantity()).isEqualTo(7);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(OrderStatus.PLACED.name());
        assertThat(response.totalAmount()).isEqualByComparingTo("1061.50");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void placeOrder_snapshotsNameAndPriceOnTheOrderLine() {
        Product phone = product(1, "Phone", "500.00", 5, true);
        stubUser();
        stubSave();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(phone));

        service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 1)));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(captor.capture());
        OrderItem line = captor.getValue().getItems().get(0);
        assertThat(line.getProductName()).isEqualTo("Phone");
        assertThat(line.getUnitPrice()).isEqualByComparingTo("500.00");
        assertThat(captor.getValue().getUser()).isSameAs(buyer);
    }

    @Test
    void placeOrder_mergesDuplicateLinesForTheSameProduct() {
        Product phone = product(1, "Phone", "100.00", 10, true);
        stubUser();
        stubSave();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(phone));

        OrderResponse response = service.placeOrder(EMAIL,
                order(new OrderItemRequest(1L, 2), new OrderItemRequest(1L, 3)));

        assertThat(phone.getStockQuantity()).isEqualTo(5);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo("500.00");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(productRepo).findAllByIdForUpdate(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrderElementsOf(Set.of(1L));
    }

    @Test
    void placeOrder_allowsBuyingExactlyTheRemainingStock() {
        Product phone = product(1, "Phone", "100.00", 3, true);
        stubUser();
        stubSave();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(phone));

        service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 3)));

        assertThat(phone.getStockQuantity()).isZero();
    }

    @Test
    void placeOrder_insufficientStock_throwsConflictAndSavesNothing() {
        Product phone = product(1, "Phone", "100.00", 2, true);
        stubUser();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(phone));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 3))));

        assertThat(ex.getMessage()).contains("Only 2").contains("Phone");
        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_rejectedLateInTheOrder_doesNotTouchStockOfEarlierLines() {
        Product plenty = product(1, "Plenty", "10.00", 50, true);
        Product scarce = product(2, "Scarce", "10.00", 1, true);
        stubUser();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(plenty, scarce));

        assertThrows(InsufficientStockException.class,
                () -> service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 5), new OrderItemRequest(2L, 2))));

        assertThat(plenty.getStockQuantity()).isEqualTo(50);
        assertThat(scarce.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void placeOrder_unavailableProduct_throwsConflict() {
        Product phone = product(1, "Phone", "100.00", 50, false);
        stubUser();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(phone));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 1))));

        assertThat(ex.getMessage()).contains("unavailable");
        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_unknownProduct_throwsNotFound() {
        stubUser();
        when(productRepo.findAllByIdForUpdate(any())).thenReturn(List.of(product(1, "Phone", "10.00", 5, true)));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 1), new OrderItemRequest(42L, 1))));

        assertThat(ex.getMessage()).contains("42");
        verify(orderRepo, never()).save(any());
    }

    @Test
    void placeOrder_unknownUser_throwsNotFound() {
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.placeOrder(EMAIL, order(new OrderItemRequest(1L, 1))));

        verify(productRepo, never()).findAllByIdForUpdate(any());
    }

    @Test
    void getOrdersForUser_mapsOrders() {
        Order stored = storedOrder(7L, "someone@shop.com");
        when(orderRepo.findByUserEmailOrderByCreatedAtDescIdDesc(EMAIL)).thenReturn(List.of(stored));

        List<OrderResponse> orders = service.getOrdersForUser(EMAIL);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).id()).isEqualTo(7L);
        assertThat(orders.get(0).items()).hasSize(1);
    }

    @Test
    void getOrder_owner_canReadIt() {
        when(orderRepo.findWithItemsById(7L)).thenReturn(Optional.of(storedOrder(7L, EMAIL)));

        assertThat(service.getOrder(7L, EMAIL, false).id()).isEqualTo(7L);
    }

    @Test
    void getOrder_otherUser_getsNotFound() {
        when(orderRepo.findWithItemsById(7L)).thenReturn(Optional.of(storedOrder(7L, "someone@shop.com")));

        assertThrows(ResourceNotFoundException.class, () -> service.getOrder(7L, EMAIL, false));
    }

    @Test
    void getOrder_admin_canReadAnyOrder() {
        when(orderRepo.findWithItemsById(7L)).thenReturn(Optional.of(storedOrder(7L, "someone@shop.com")));

        assertThat(service.getOrder(7L, "admin@shop.com", true).id()).isEqualTo(7L);
    }

    @Test
    void getOrder_missing_throwsNotFound() {
        when(orderRepo.findWithItemsById(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOrder(7L, EMAIL, true));
    }

    private Order storedOrder(long id, String ownerEmail) {
        Order order = new Order();
        order.setId(id);
        order.setUser(User.builder().id(9L).email(ownerEmail).role(Role.USER).build());
        order.setStatus(OrderStatus.PLACED);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("50.00"));
        OrderItem line = new OrderItem();
        line.setProductName("Thing");
        line.setUnitPrice(new BigDecimal("50.00"));
        line.setQuantity(1);
        order.addItem(line);
        return order;
    }
}
