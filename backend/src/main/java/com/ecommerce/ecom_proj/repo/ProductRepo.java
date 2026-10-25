package com.ecommerce.ecom_proj.repo;

import com.ecommerce.ecom_proj.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    // The keyword must already have its LIKE wildcards escaped with a backslash (see ProductService).
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'")
    List<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);

    // Row-level write lock so concurrent checkouts cannot both read the same stock and oversell.
    // Ordered by id so every transaction locks rows in the same order (avoids deadlocks).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
