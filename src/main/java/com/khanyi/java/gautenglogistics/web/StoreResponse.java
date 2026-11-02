package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;

/** A store, as JSON. */
public record StoreResponse(long id, String name, Coordinate location) {

    static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getName(), store.getLocation());
    }
}
