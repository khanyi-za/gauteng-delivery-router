package com.khanyi.java.gautenglogistics.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What POST /delivery-quotes and POST /orders expect, as JSON. Example:
 * <pre>
 * {"lat": -26.1076, "lon": 28.0567, "items": [{"sku": "SNEAKER-WHT-8", "quantity": 1}]}
 * </pre>
 *
 * @param lat     where the customer is
 * @param lon     where the customer is
 * @param items   what they want
 * @param express for orders: true for express delivery. Optional, false if left out. Quotes ignore it.
 */
record DeliveryQuoteRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
                            @NotNull @DecimalMin("-180") @DecimalMax("180") Double lon,
                            @NotEmpty List<@Valid Item> items,
                            Boolean express) {

    record Item(@NotBlank String sku, @Positive int quantity) {
    }

    /**
     * True for express delivery. A Boolean (not boolean) field, so leaving "express" out of the JSON is
     * allowed: Jackson 3 refuses a missing value for a plain boolean.
     */
    boolean isExpress() {
        return Boolean.TRUE.equals(express);
    }

    /** The quantities by product code. If a product is listed twice, the quantities are added up. */
    Map<String, Integer> quantitiesBySku() {
        return items.stream().collect(Collectors.toMap(Item::sku, Item::quantity, Integer::sum));
    }
}
