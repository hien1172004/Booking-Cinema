package org.example.cinemaBooking.DTO.Response.Combo;

public record ComboItemResponse(
        String productId,
        String productName,
        Integer quantity
) {}