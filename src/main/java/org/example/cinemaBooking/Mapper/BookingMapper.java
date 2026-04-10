package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Booking.BookingSummaryResponse;
import org.example.cinemaBooking.Entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;



@Mapper(componentModel = "spring",
        uses = {TicketMapper.class, ProductMapper.class, ShowtimeMapper.class})
public interface BookingMapper {

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "showtime", source = "showtime")
    @Mapping(target = "tickets", source = "tickets")
    @Mapping(target = "products", source = "bookingProducts")
    @Mapping(target = "paymentUrl", ignore = true)
    BookingResponse toResponse(Booking booking);

    // Summary
    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "startTime", source = "showtime.startTime")
    @Mapping(target = "seatCount", expression = "java(booking.getTickets() != null ? booking.getTickets().size() : 0)")
    BookingSummaryResponse toSummary(Booking booking);
}