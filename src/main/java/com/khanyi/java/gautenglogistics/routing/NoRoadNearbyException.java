package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;

/** Thrown when a GPS point isn't close enough to any road we can deliver on. */
public class NoRoadNearbyException extends RuntimeException {

  public NoRoadNearbyException(Coordinate point) {
    super("No road found near " + point.lat() + ", " + point.lon());
  }
}
