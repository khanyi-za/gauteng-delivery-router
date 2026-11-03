package com.khanyi.java.gautenglogistics.dispatch;

/** What a driver is doing right now. */
public enum DriverStatus {

    /** Waiting at their store for an order. */
    AT_STORE,

    /** Driving an order to the customer. */
    DELIVERING,

    /** Driving back to the store after a delivery. */
    RETURNING
}
