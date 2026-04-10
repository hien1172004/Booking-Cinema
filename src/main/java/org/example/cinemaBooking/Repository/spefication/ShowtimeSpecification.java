package org.example.cinemaBooking.Repository.spefication;

import jakarta.persistence.criteria.*;
import org.example.cinemaBooking.DTO.Request.Showtime.ShowtimeFilterRequest;
import org.example.cinemaBooking.Entity.*;

import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ShowtimeSpecification {

    // Constants for entity field names
    private static final String FIELD_MOVIE = "movie";
    private static final String FIELD_ROOM = "room";
    private static final String FIELD_CINEMA = "cinema";
    private static final String FIELD_ID = "id";
    private static final String FIELD_MOVIE_TITLE = "title";
    private static final String FIELD_CINEMA_NAME = "name";
    private static final String FIELD_DELETED_AT = "deletedAt";
    private static final String FIELD_START_TIME = "startTime";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_STATUS = "status";

    private ShowtimeSpecification() {}

    public static Specification<Showtime> of(ShowtimeFilterRequest f) {
        return (root, query, cb) -> {
            // Setup fetches - always use fetch for relationships
            if (Long.class != query.getResultType()) {
                Fetch<Showtime, Movie> movieFetch = root.fetch(FIELD_MOVIE, JoinType.LEFT);
                Fetch<Showtime, Room> roomFetch = root.fetch(FIELD_ROOM, JoinType.LEFT);
                roomFetch.fetch(FIELD_CINEMA, JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy bản ghi chưa soft-delete
            predicates.add(cb.isNull(root.get(FIELD_DELETED_AT)));

            // Movie filter
            if (isNotBlank(f.movieId())) {
                predicates.add(cb.equal(root.get(FIELD_MOVIE).get(FIELD_ID), f.movieId()));
            }

            // Room filter
            if (isNotBlank(f.roomId())) {
                predicates.add(cb.equal(root.get(FIELD_ROOM).get(FIELD_ID), f.roomId()));
            }

            // Cinema filter - use get instead of join since we already fetched
            if (isNotBlank(f.cinemaId())) {
                predicates.add(cb.equal(root.get(FIELD_ROOM).get(FIELD_CINEMA).get(FIELD_ID), f.cinemaId()));
            }

            // Date filter
            if (f.date() != null) {
                LocalDateTime from = f.date().atStartOfDay();
                LocalDateTime to = from.plusDays(1);
                predicates.add(cb.between(root.get(FIELD_START_TIME), from, to));
            }

            // Language filter
            if (f.language() != null) {
                predicates.add(cb.equal(root.get(FIELD_LANGUAGE), f.language()));
            }

            // Status filter
            if (f.status() != null) {
                predicates.add(cb.equal(root.get(FIELD_STATUS), f.status()));
            }

            // Keyword search
            if (isNotBlank(f.keyword())) {
                String searchPattern = "%" + f.keyword().toLowerCase() + "%";

                Predicate moviePredicate = cb.like(
                        cb.lower(root.get(FIELD_MOVIE).get(FIELD_MOVIE_TITLE)),
                        searchPattern
                );

                Predicate cinemaPredicate = cb.like(
                        cb.lower(root.get(FIELD_ROOM).get(FIELD_CINEMA).get(FIELD_CINEMA_NAME)),
                        searchPattern
                );

                predicates.add(cb.or(moviePredicate, cinemaPredicate));
            }

            // Order by
            query.orderBy(cb.asc(root.get(FIELD_START_TIME)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }
}