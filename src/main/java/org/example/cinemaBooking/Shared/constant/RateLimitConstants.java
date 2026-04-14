package org.example.cinemaBooking.Shared.constant;

public class RateLimitConstants {
    public static final int CAPACITY = 100;
    public static final int REFILL = 100;
    public static final int REFILL_DURATION_MINUTES = 1;

    // Headers
    public static final String HEADER_LIMIT     = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RETRY     = "Retry-After";

    // Key prefixes
    public static final String PREFIX_GLOBAL = "global";
    public static final String PREFIX_API    = "api";
}