package io.github.eladiolramos.clickandeat.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogMapper catalogMapper;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository, CatalogMapper catalogMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.catalogMapper = catalogMapper;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(catalogMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> getAvailableProductsByCategory(Long categoryId) {
        return productRepository.findAllByCategoryIdAndAvailableTrueOrderByNameAsc(categoryId)
                .stream()
                .map(catalogMapper::toResponse)
                .toList();
    }

    public List<ProductResponse> getAvailableProducts() {
        return productRepository.findAllAvailableWithCategory()
                .stream()
                .map(catalogMapper::toResponse)
                .toList();
    }


}
