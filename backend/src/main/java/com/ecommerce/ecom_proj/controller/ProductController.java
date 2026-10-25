package com.ecommerce.ecom_proj.controller;

import com.ecommerce.ecom_proj.dto.ProductRequest;
import com.ecommerce.ecom_proj.dto.ProductResponse;
import com.ecommerce.ecom_proj.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Browse the catalog (public). Create, update and delete require the ADMIN role.")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List products",
            description = "Paginated and sortable, e.g. ?page=0&size=8&sort=price,desc. Optional category filter.")
    public Page<ProductResponse> getProducts(@RequestParam(required = false) String category,
                                             @ParameterObject @PageableDefault(size = 8, sort = "id") Pageable pageable) {
        return service.getProducts(category, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ProductResponse getProduct(@PathVariable Long id) {
        return service.getProductById(id);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products",
            description = "Case-insensitive match on name, description, brand and category. Returns at most 10 results.")
    public List<ProductResponse> searchProducts(@RequestParam String keyword) {
        return service.searchProducts(keyword);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a product (ADMIN)")
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = service.addProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a product (ADMIN)")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a product (ADMIN)")
    public void deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
    }
}
