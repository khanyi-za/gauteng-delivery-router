package com.khanyi.java.gautenglogistics.domain;

import jakarta.persistence.Embeddable;

/**
 * How many of one product an order wants. It has no id of its own: it's stored as a row of the
 * order_item table and only exists as part of its order.
 */
@Embeddable
public record OrderItem(String sku, int quantity) {
}
