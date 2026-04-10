package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Payment.PaymentResponse;
import org.example.cinemaBooking.Entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "paymentId",  source = "id")
    @Mapping(target = "bookingId",  source = "booking.id")
    @Mapping(target = "bookingCode",source = "booking.bookingCode")
    @Mapping(target = "paymentUrl", ignore = true)   // service inject
    @Mapping(target = "createdAt",  source = "createdAt")
    PaymentResponse toResponse(Payment payment);
}