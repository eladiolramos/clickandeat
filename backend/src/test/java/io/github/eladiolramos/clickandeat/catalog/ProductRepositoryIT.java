package io.github.eladiolramos.clickandeat.catalog;

import io.github.eladiolramos.clickandeat.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class})
public class ProductRepositoryIT {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void findsOnlyAvailableProductsOfCategoryOrderedByName() {

        //Arrange
        Category drink = categoryRepository.save(new Category("Bebidas"));
        Category dessert = categoryRepository.save(new Category("Postres"));

        productRepository.save(new Product("Fanta", new BigDecimal("2.35"), drink));
        productRepository.save(new Product("Agua", new BigDecimal("1.95"), drink));
        Product zumo = new Product("Zumo", new BigDecimal("2.35"), drink);
        zumo.setAvailable(false);
        productRepository.save(zumo);
        productRepository.save(new Product("Tarta", new BigDecimal("6.50"), dessert));

        testEntityManager.flush();
        testEntityManager.clear();

        //Act
        List<Product> drinks = productRepository.findAllByCategoryIdAndAvailableTrueOrderByNameAsc(drink.getId());

        //Assert
        assertThat(drinks).extracting(Product::getName).containsExactly("Agua", "Fanta");
    }

    @Test
    void rejectsNegativePrice() {

        //Arrange
        Category drink = categoryRepository.save(new Category("Bebidas"));

        //Act and Assert
        assertThatThrownBy(() -> productRepository.saveAndFlush(new Product("Fanta", new BigDecimal("-1.00"), drink)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cannotDeleteCategoryWithProduct() {

        //Arrange
        Category drink = categoryRepository.save(new Category("Bebidas"));
        productRepository.save(new Product("Fanta", new BigDecimal("2.35"), drink));
        testEntityManager.flush();
        testEntityManager.clear();
        Long categoryId = drink.getId();

        //Act and Assert
        assertThatThrownBy(() -> {
            categoryRepository.deleteById(categoryId);
            categoryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void loadsAvailableProductsWithCategoryInOneQuery() {

        //Arrange
        Category drink = categoryRepository.save(new Category("Bebidas"));
        Category dessert = categoryRepository.save(new Category("Postres"));
        productRepository.save(new Product("Fanta", new BigDecimal("2.35"), drink));
        productRepository.save(new Product("Agua", new BigDecimal("1.95"), drink));
        productRepository.save(new Product("Tarta Queso", new BigDecimal("6.50"), dessert));
        productRepository.save(new Product("Helado", new BigDecimal("7.95"), dessert));
        Product zumo = new Product("Zumo", new BigDecimal("2.35"), drink);
        zumo.setAvailable(false);
        productRepository.save(zumo);

        testEntityManager.flush();
        testEntityManager.clear();

        //Act
        List<Product> products = productRepository.findAllAvailableWithCategory();

        //Assert
        assertThat(products).extracting(Product::getName).containsExactly("Agua", "Fanta", "Helado", "Tarta Queso");
        assertThat(products).extracting(product -> product.getCategory().getName()).containsExactly("Bebidas", "Bebidas", "Postres", "Postres");
    }
}
