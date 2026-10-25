package com.ecommerce.ecom_proj.service;

import com.ecommerce.ecom_proj.dto.ProductRequest;
import com.ecommerce.ecom_proj.dto.ProductResponse;
import com.ecommerce.ecom_proj.exception.ResourceNotFoundException;
import com.ecommerce.ecom_proj.model.Product;
import com.ecommerce.ecom_proj.repo.ProductRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo repo;

    @InjectMocks
    private ProductService service;

    private static Product product(long id, String name) {
        return Product.builder().id(id).name(name).description("desc").brand("brand")
                .price(new BigDecimal("99.50")).category("Laptop").releaseDate(LocalDate.of(2024, 1, 15))
                .productAvailable(true).stockQuantity(10).build();
    }

    private static ProductRequest request(String name) {
        return new ProductRequest(name, "A description", "Acme", new BigDecimal("49.99"), "Mobile",
                LocalDate.of(2024, 5, 1), true, 25);
    }

    @Test
    void getProducts_withoutCategory_returnsAllProductsPage() {
        Pageable pageable = PageRequest.of(0, 8);
        when(repo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product(1, "A"), product(2, "B")), pageable, 2));

        Page<ProductResponse> result = service.getProducts(null, pageable);

        assertThat(result.getContent()).extracting(ProductResponse::name).containsExactly("A", "B");
        verify(repo, never()).findByCategoryIgnoreCase(any(), any());
    }

    @Test
    void getProducts_blankCategory_isTreatedAsNoFilter() {
        Pageable pageable = PageRequest.of(0, 8);
        when(repo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product(1, "A")), pageable, 1));

        service.getProducts("   ", pageable);

        verify(repo).findAll(pageable);
    }

    @Test
    void getProducts_withCategory_filtersCaseInsensitivelyAndTrims() {
        Pageable pageable = PageRequest.of(1, 4);
        when(repo.findByCategoryIgnoreCase("Laptop", pageable))
                .thenReturn(new PageImpl<>(List.of(product(3, "C")), pageable, 5));

        Page<ProductResponse> result = service.getProducts(" Laptop ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getProductById_returnsMappedProduct() {
        when(repo.findById(1L)).thenReturn(Optional.of(product(1, "Phone")));

        ProductResponse response = service.getProductById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Phone");
        assertThat(response.price()).isEqualByComparingTo("99.50");
    }

    @Test
    void getProductById_missing_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getProductById(99L));

        assertThat(ex.getMessage()).contains("99");
    }

    @Test
    void addProduct_mapsRequestAndTrimsName() {
        when(repo.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });

        ProductResponse response = service.addProduct(request("  Galaxy  "));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Galaxy");
        assertThat(response.brand()).isEqualTo("Acme");
        assertThat(response.stockQuantity()).isEqualTo(25);
        assertThat(response.productAvailable()).isTrue();
    }

    @Test
    void updateProduct_overwritesExistingFields() {
        Product existing = product(5, "Old name");
        when(repo.findById(5L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        ProductResponse response = service.updateProduct(5L, request("New name"));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.category()).isEqualTo("Mobile");
        assertThat(existing.getStockQuantity()).isEqualTo(25);
    }

    @Test
    void updateProduct_missing_throwsNotFoundAndNeverSaves() {
        when(repo.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateProduct(5L, request("x")));

        verify(repo, never()).save(any());
    }

    @Test
    void deleteProduct_removesExistingProduct() {
        Product existing = product(2, "Gone");
        when(repo.findById(2L)).thenReturn(Optional.of(existing));

        service.deleteProduct(2L);

        verify(repo).delete(existing);
    }

    @Test
    void deleteProduct_missing_throwsNotFound() {
        when(repo.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteProduct(2L));

        verify(repo, never()).delete(any(Product.class));
    }

    @Test
    void searchProducts_blankKeyword_returnsEmptyWithoutQuerying() {
        assertThat(service.searchProducts("  ")).isEmpty();
        assertThat(service.searchProducts(null)).isEmpty();

        verify(repo, never()).searchProducts(any(), any());
    }

    @Test
    void searchProducts_trimsKeywordAndCapsResultsAtTen() {
        when(repo.searchProducts(eq("phone"), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(product(1, "Phone")));

        List<ProductResponse> results = service.searchProducts("  phone ");

        assertThat(results).extracting(ProductResponse::name).containsExactly("Phone");
    }

    @Test
    void searchProducts_escapesLikeWildcardsSoTheyMatchLiterally() {
        when(repo.searchProducts(eq("50\\%\\_off\\\\"), any(Pageable.class))).thenReturn(List.of());

        service.searchProducts("50%_off\\");

        verify(repo).searchProducts(eq("50\\%\\_off\\\\"), any(Pageable.class));
    }
}
