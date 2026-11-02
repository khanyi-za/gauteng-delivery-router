package com.khanyi.java.gautenglogistics.routing;

/** Thrown when no single store has everything in an order. */
public class NoStoreHasStockException extends RuntimeException {

  public NoStoreHasStockException() {
    super("No store has everything in this order");
  }
}
