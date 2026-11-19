package io.github.eladiolramos.clickandeat.catalog;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        String categoryName
) {
}
