-- Menu categories (e.g. Hamburguesas, Bebidas, Postres...).
CREATE TABLE category (
    id         BIGINT GENERATED ALWAYS AS IDENTITY,
    name       VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uk_category_name UNIQUE (name)
);
