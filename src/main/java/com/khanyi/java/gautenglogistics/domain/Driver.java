package com.khanyi.java.gautenglogistics.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;

/** A driver who delivers orders from one store. */
@Entity
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A plain id, like StoreStock: dispatch only needs to group drivers by store
    private Long storeId;

    private String name;

    /** When they're next back at the store, ready for another delivery. */
    private Instant freeAt;

    // The order from their latest trip, or empty if they haven't made one. A plain id, like storeId
    private Long latestOrderId;

    /** For JPA. */
    protected Driver() {
    }

    public Driver(Long storeId, String name, Instant freeAt) {
        this.storeId = storeId;
        this.name = name;
        this.freeAt = freeAt;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getName() {
        return name;
    }

    public Instant getFreeAt() {
        return freeAt;
    }

    /** The order from their latest trip. They're still on that trip until freeAt. */
    public Long getLatestOrderId() {
        return latestOrderId;
    }

    /** They leave the store with an order, and will be back at the given time. */
    public void startTrip(Long orderId, Instant backAt) {
        this.latestOrderId = orderId;
        this.freeAt = backAt;
    }
}
