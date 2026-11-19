package io.github.eladiolramos.clickandeat.catalog;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

    CategoryResponse toResponse(Category category);

    @Mapping(target = "categoryName", source = "category.name")
    ProductResponse toResponse(Product product);
}
