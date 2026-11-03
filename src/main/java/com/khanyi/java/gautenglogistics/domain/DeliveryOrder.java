package com.khanyi.java.gautenglogistics.domain;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** An order a customer has placed, to be delivered from one store. */
@Entity
@Table(name = "delivery_order")
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A link to the Store object (not just its id), because showing an order shows the store's name
    @ManyToOne(optional = false)
    @JoinColumn(name = "store_id") // specify the foreign key column
    private Store store;

    private double customerLat;
    private double customerLon;

    @Enumerated(EnumType.STRING) // saved as text, e.g. 'WAITING_FOR_DRIVER', so the table is easy to read
    private OrderStatus status;

    private Instant placedAt;
    private Instant deliverBy;

    @Column(name = "drive_time_s") // without this JPA would look for a column called drive_times
    private double driveTimeS;

    // Always loaded with the order, because an order is never shown without what's in it
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    /** For JPA. */
    protected DeliveryOrder() {
    }

    /**
     * A new order, waiting for a driver.
     *
     * @param driveTimeS how long the drive from the store to the customer takes, in seconds
     */
    public DeliveryOrder(Store store, Coordinate customer, List<OrderItem> items,
                         Instant placedAt, Instant deliverBy, double driveTimeS) {
        this.store = store;
        this.customerLat = customer.lat();
        this.customerLon = customer.lon();
        this.items = new ArrayList<>(items);
        this.placedAt = placedAt;
        this.deliverBy = deliverBy;
        this.driveTimeS = driveTimeS;
        this.status = OrderStatus.WAITING_FOR_DRIVER;
    }

    public Long getId() {
        return id;
    }

    public Store getStore() {
        return store;
    }

    public Coordinate getCustomer() {
        return new Coordinate(customerLat, customerLon);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getDeliverBy() {
        return deliverBy;
    }

    public double getDriveTimeS() {
        return driveTimeS;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
