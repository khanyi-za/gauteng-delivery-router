package com.khanyi.java.gautenglogistics.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Something the retailer sells, such as "White T-shirt, size M". */
@Entity
public class Product {

    /** The retailer's own product code, such as TSHIRT-WHT-M. */
    @Id
    private String sku;

    private String name;

    /** For JPA. */
    protected Product() {
    }

    public Product(String sku, String name) {
        this.sku = sku;
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }
}
