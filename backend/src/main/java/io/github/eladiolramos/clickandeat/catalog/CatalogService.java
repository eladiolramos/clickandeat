package io.github.eladiolramos.clickandeat.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly=true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public List<Product> getAvailableProductsByCategory(Long categoryId) {
        return productRepository.findAllByCategoryIdAndAvailableTrueOrderByNameAsc(categoryId);
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findAllAvailableWithCategory();
    }


}
