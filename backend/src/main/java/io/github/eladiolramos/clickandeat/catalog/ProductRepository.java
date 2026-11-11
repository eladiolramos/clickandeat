package io.github.eladiolramos.clickandeat.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p FROM Product p JOIN FETCH p.category c
            WHERE p.available = true
            ORDER BY c.name, p.name
            """)
    List<Product> findAllAvailableWithCategory();

    List<Product> findAllByCategoryIdAndAvailableTrueOrderByNameAsc(Long categoryId);
}
