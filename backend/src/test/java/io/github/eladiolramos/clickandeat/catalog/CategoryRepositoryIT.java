package io.github.eladiolramos.clickandeat.catalog;

import io.github.eladiolramos.clickandeat.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.shaded.org.yaml.snakeyaml.constructor.DuplicateKeyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({TestcontainersConfiguration.class})
public class CategoryRepositoryIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void savesCategoryWithGeneratedIdAndAuditDates() {

        //Arrange
        Category category = new Category("Complementos");

        //Act
        Category savedCategory = categoryRepository.saveAndFlush(category);

        //Assert
        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getCreatedAt()).isNotNull();
        assertThat(savedCategory.getUpdatedAt()).isNotNull();
    }

    @Test
    void findsCategoryByName() {

        //Arrange
        categoryRepository.save(new Category("Bebidas"));
        testEntityManager.flush();
        testEntityManager.clear();

        //Act
        Optional<Category> findBebidas = categoryRepository.findByNameIgnoreCase("bebidas");
        Optional<Category> findNoExiste =  categoryRepository.findByNameIgnoreCase("NoExiste");

        //Assert
        assertThat(findBebidas).isPresent();
        assertThat(findNoExiste).isEmpty();
    }

    @Test
    void rejectsDuplicateCategoryName() {

        //Arrange

        categoryRepository.saveAndFlush(new Category("Bebidas"));

        //Act and Assert
        assertThatThrownBy(()->categoryRepository.saveAndFlush(new Category("Bebidas"))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
