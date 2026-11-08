-- Menu products. Price may be 0 for free extras (sauces, etc.).
CREATE TABLE product (
    id          BIGINT GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100)    NOT NULL,
    description TEXT,
    image_url   TEXT,
    price       NUMERIC(10, 2) NOT NULL,
    available   BOOLEAN        NOT NULL DEFAULT TRUE,
    category_id BIGINT         NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT uk_product_name UNIQUE (name),
    CONSTRAINT ck_product_price_non_negative CHECK (price >= 0),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT
);

CREATE INDEX idx_product_category_id ON product (category_id);
