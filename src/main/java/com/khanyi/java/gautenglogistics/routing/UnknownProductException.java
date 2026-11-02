package com.khanyi.java.gautenglogistics.routing;

import java.util.Collection;

/** Thrown when an order asks for a product code the retailer doesn't sell. */
public class UnknownProductException extends RuntimeException {

  public UnknownProductException(Collection<String> skus) {
    super("Unknown product code(s): " + String.join(", ", skus));
  }
}
