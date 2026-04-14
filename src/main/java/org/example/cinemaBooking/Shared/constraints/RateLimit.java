package org.example.cinemaBooking.Shared.constraints;

import org.example.cinemaBooking.Shared.constant.RateLimitConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int capacity() default 50;
    int refillPerMinute() default 50;
    String keyPrefix() default RateLimitConstants.PREFIX_API;
}