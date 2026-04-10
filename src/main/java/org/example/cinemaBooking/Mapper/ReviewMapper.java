package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Movie.ReviewRequest;
import org.example.cinemaBooking.DTO.Response.Review.ReviewResponse;
import org.example.cinemaBooking.DTO.Response.Review.ReviewSummaryResponse;

import org.example.cinemaBooking.Entity.Review;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "user", ignore = true)
    Review toEntity(ReviewRequest reviewRequest);

    // Cập nhật entity từ request (không thay đổi movie/user)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateFromRequest(ReviewRequest request, @MappingTarget Review review);

    @Mapping(source = "movie.id", target = "movieId")
    @Mapping(source = "movie.title", target = "movieTitle")   // Cần fetch movie.title trước!
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")   // Cần fetch user.username
    ReviewResponse toResponse(Review review);


    @Mapping(source = "user.username", target = "username")
    @Mapping(target = "commentTruncated", expression = "java( truncateComment(review.getComment()) )")
    ReviewSummaryResponse toSummary(Review review);

    // Custom method truncate
    default String truncateComment(String comment) {
        if (comment == null || comment.length() <= 100) {
            return comment;
        }
        return comment.substring(0, 97) + "...";
    }

}

