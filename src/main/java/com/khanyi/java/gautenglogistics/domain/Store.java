package com.khanyi.java.gautenglogistics.domain;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** One of the retailer's shops. Deliveries leave from here. */
@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double lat;
    private double lon;

    /** For JPA, which creates the object first and then fills in the fields. */
    protected Store() {
    }

    public Store(String name, Coordinate location) {
        this.name = name;
        this.lat = location.lat();
        this.lon = location.lon();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinate getLocation() {
        return new Coordinate(lat, lon);
    }
}
