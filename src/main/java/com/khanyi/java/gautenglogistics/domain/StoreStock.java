package com.khanyi.java.gautenglogistics.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/** How many of one product one store has. */
@Entity
public class StoreStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain ids rather than links to the Store and Product objects: store selection only needs the ids
    private Long storeId;
    private String sku;

    private int quantity;

    /**
     * Goes up by one every time this row is saved. When saving, JPA checks it hasn't changed since
     * the row was read, so if two dispatch runs take the same stock at once, the second one fails
     * instead of selling stock that's already gone.
     */
    @Version
    private long version;

    /** For JPA. */
    protected StoreStock() {
    }

    public StoreStock(Long storeId, String sku, int quantity) {
        this.storeId = storeId;
        this.sku = sku;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getVersion() {
        return version;
    }
}
