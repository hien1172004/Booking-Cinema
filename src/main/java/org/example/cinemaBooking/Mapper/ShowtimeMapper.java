package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Showtime.CreateShowtimeRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.UpdateShowtimeRequest;
import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeDetailResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSummaryResponse;
import org.example.cinemaBooking.Entity.Category;
import org.example.cinemaBooking.Entity.Showtime;

import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ShowtimeMapper {

    // ── Entity → Summary Response (dùng cho list) ────────────────────
    @Mapping(source = "movie.id",       target = "movieId")
    @Mapping(source = "movie.title",    target = "movieTitle")
    @Mapping(source = "movie.posterUrl",target = "posterUrl")
    @Mapping(source = "movie.duration", target = "durationMinutes")
    @Mapping(source = "room.id",        target = "roomId")
    @Mapping(source = "room.name",      target = "roomName")
    @Mapping(source = "room.cinema.id", target = "cinemaId")
    @Mapping(source = "room.cinema.name", target = "cinemaName")
    @Mapping(target = "endTime",        expression = "java(showtime.getEndTime())")
    @Mapping(target = "bookable",       expression = "java(showtime.isBookable())")
    ShowtimeSummaryResponse toSummaryResponse(Showtime showtime);

    List<ShowtimeSummaryResponse> toSummaryResponseList(List<Showtime> showtimes);

    // ── Entity → Detail Response (dùng cho single detail) ────────────
    @Mapping(source = "movie.id",          target = "movieId")
    @Mapping(source = "movie.title",       target = "movieTitle")
    @Mapping(source = "movie.posterUrl",   target = "posterUrl")
    @Mapping(source = "movie.duration",    target = "durationMinutes")
    @Mapping(source = "movie.categories",       target = "category", qualifiedByName = "categoriesToCommaSeparated")
    @Mapping(source = "movie.ageRating",       target = "rating")
    @Mapping(source = "room.id",           target = "roomId")
    @Mapping(source = "room.name",         target = "roomName")
    @Mapping(source = "room.roomType",     target = "roomType")
    @Mapping(source = "room.cinema.id",    target = "cinemaId")
    @Mapping(source = "room.cinema.name",  target = "cinemaName")
    @Mapping(source = "room.cinema.address", target = "cinemaAddress")
    @Mapping(target = "endTime",           expression = "java(showtime.getEndTime())")
    @Mapping(target = "bookable",          expression = "java(showtime.isBookable())")
    @Mapping(target = "ongoing",           expression = "java(showtime.isOngoing())")
    @Mapping(target = "finished",          expression = "java(showtime.isFinished())")
    ShowtimeDetailResponse toDetailResponse(Showtime showtime);

    // movie và room sẽ được set thủ công trong service (cần fetch từ DB)
    @Mapping(target = "movie",         ignore = true)
    @Mapping(target = "room",          ignore = true)
    @Mapping(target = "showtimeSeats", ignore = true)
    @Mapping(target = "availableSeats",ignore = true)
    @Mapping(target = "status",        ignore = true) // default SCHEDULED
    Showtime toEntity(CreateShowtimeRequest request);

    // ── UpdateRequest → Entity (partial update) ───────────────────────
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "movie",         ignore = true)
    @Mapping(target = "room",          ignore = true)
    @Mapping(target = "showtimeSeats", ignore = true)
    @Mapping(target = "availableSeats",ignore = true)
    void updateEntityFromRequest(UpdateShowtimeRequest request, @MappingTarget Showtime showtime);


    @Mapping(target = "showtimeId", source = "id")
    @Mapping(target = "movieTitle", source = "movie.title")
    @Mapping(target = "roomName", source = "room.name")
    @Mapping(target = "cinemaName", source = "room.cinema.name")
    @Mapping(target = "startTime", source = "startTime")
    BookingResponse.ShowtimeInfo toShowtimeInfo(Showtime showtime);


    @Named("categoriesToCommaSeparated")
    default String categoriesToCommaSeparated(Set<Category> categories) {  // Đổi từ List thành Set
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.joining(", "));
    }
}
