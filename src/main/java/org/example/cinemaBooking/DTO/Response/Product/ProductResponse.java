package org.example.cinemaBooking.DTO.Response.Product;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        BigDecimal price,
        String image,
        Boolean active
) {}