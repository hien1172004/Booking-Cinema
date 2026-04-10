package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Ticket.TicketResponse;
import org.example.cinemaBooking.Entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "seatRow", source = "seat.seatRow")
    @Mapping(target = "seatNumber", source = "seat.seatNumber")
    @Mapping(target = "seatType", source = "seat.seatType.name")
    BookingResponse.TicketInfo toTicketInfo(Ticket ticket);

    List<BookingResponse.TicketInfo> toTicketInfos(List<Ticket> tickets);

    @Mapping(target = "ticketId",  source = "id")
    @Mapping(target = "seat",      expression = "java(mapSeat(t))")
    @Mapping(target = "showtime",  expression = "java(mapShowtime(t))")
    TicketResponse toResponse(Ticket t);

    default TicketResponse.SeatInfo mapSeat(Ticket t) {
        return new TicketResponse.SeatInfo(
                t.getSeat().getSeatRow(),
                t.getSeat().getSeatNumber(),
                t.getSeat().getSeatType().getName()
        );
    }

    default TicketResponse.ShowtimeInfo mapShowtime(Ticket t) {
        var st = t.getBooking().getShowtime();
        return new TicketResponse.ShowtimeInfo(
                st.getMovie().getTitle(),
                st.getRoom().getCinema().getName(),
                st.getRoom().getName(),
                st.getStartTime()
        );
    }
}