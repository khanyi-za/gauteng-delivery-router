package com.khanyi.java.gautenglogistics.web;


import com.khanyi.java.gautenglogistics.routing.NoRoadNearbyException;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import com.khanyi.java.gautenglogistics.routing.UnknownProductException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns our own exceptions into HTTP errors, for every controller. */
@RestControllerAdvice
class ApiExceptionHandler {

    /**
     * The request was valid, but we can't carry it out (HTTP 422): a point too far from any road,
     * a product we don't sell, or no store with everything in the order.
     */
    @ExceptionHandler({NoRoadNearbyException.class, UnknownProductException.class, NoStoreHasStockException.class})
    ProblemDetail unprocessable(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }
}
