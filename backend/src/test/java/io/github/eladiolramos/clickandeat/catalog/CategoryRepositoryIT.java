package io.github.eladiolramos.clickandeat.catalog;

import io.github.eladiolramos.clickandeat.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

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

        //Arrange and Act
        Category savedCategory = categoryRepository.saveAndFlush(new Category("Complementos"));

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
        Optional<Category> findDrinks = categoryRepository.findByNameIgnoreCase("bebidas");
        Optional<Category> findNotExist = categoryRepository.findByNameIgnoreCase("NoExiste");

        //Assert
        assertThat(findDrinks).isPresent();
        assertThat(findNotExist).isEmpty();
    }

    @Test
    void rejectsDuplicateCategoryName() {

        //Arrange

        categoryRepository.saveAndFlush(new Category("Bebidas"));

        //Act and Assert
        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category("Bebidas"))).isInstanceOf(DataIntegrityViolationException.class);
    }
}
