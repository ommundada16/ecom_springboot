package com.ecommerce.ecom_proj.service;

import com.ecommerce.ecom_proj.dto.ProductRequest;
import com.ecommerce.ecom_proj.dto.ProductResponse;
import com.ecommerce.ecom_proj.exception.ResourceNotFoundException;
import com.ecommerce.ecom_proj.model.Product;
import com.ecommerce.ecom_proj.repo.ProductRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final int SEARCH_LIMIT = 10;

    private final ProductRepo repo;

    public ProductService(ProductRepo repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String category, Pageable pageable) {
        Page<Product> page = (category == null || category.isBlank())
                ? repo.findAll(pageable)
                : repo.findByCategoryIgnoreCase(category.trim(), pageable);
        return page.map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return ProductResponse.from(repo.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        apply(product, request);
        return ProductResponse.from(repo.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        repo.delete(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return repo.searchProducts(escapeLike(keyword.trim()), PageRequest.of(0, SEARCH_LIMIT)).stream()
                .map(ProductResponse::from)
                .toList();
    }

    // Users type plain text, so '%' and '_' must match literally instead of acting as SQL wildcards.
    private static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Product findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setCategory(request.category());
        product.setReleaseDate(request.releaseDate());
        product.setProductAvailable(request.productAvailable());
        product.setStockQuantity(request.stockQuantity());
    }
}
