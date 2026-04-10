package org.example.cinemaBooking.Repository.spefication;

import jakarta.persistence.criteria.Join;
import org.example.cinemaBooking.Entity.Category;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Shared.enums.AgeRating;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.springframework.data.jpa.domain.Specification;

public class MovieSpecification {

    public static Specification<Movie> filterMovie(
            String keyword,
            MovieStatus status,
            String categoryId,
            AgeRating ageRating
    ) {

        return (root, query, cb) -> {

            var predicates = cb.conjunction();

            if(keyword != null && !keyword.isEmpty()){
                predicates = cb.and(
                        predicates,
                        cb.like(cb.lower(root.get("title")),
                                "%" + keyword.toLowerCase() + "%")
                );
            }

            if(status != null){
                predicates = cb.and(
                        predicates,
                        cb.equal(root.get("status"), status)
                );
            }

            if(ageRating != null){
                predicates = cb.and(
                        predicates,
                        cb.equal(root.get("ageRating"), ageRating)
                );
            }

            if(categoryId != null){
                Join<Movie, Category> categoryJoin = root.join("categories");
                assert query != null;
                query.distinct(true);
                predicates = cb.and(
                        predicates,
                        cb.equal(categoryJoin.get("id"), categoryId)
                );
            }

            predicates = cb.and(
                    predicates,
                    cb.isFalse(root.get("deleted"))
            );

            return predicates;
        };
    }
}