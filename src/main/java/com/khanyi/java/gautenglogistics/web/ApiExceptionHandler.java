package com.khanyi.java.gautenglogistics.web;


import com.khanyi.java.gautenglogistics.routing.NoRoadNearbyException;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import com.khanyi.java.gautenglogistics.routing.UnknownProductException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns errors from the services into HTTP errors, for every controller. */
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

    /** Other orders kept taking the same stock at the same moment, even after trying again (HTTP 409). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail conflict(OptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The stock changed while placing the order. Please try again.");
    }
}
