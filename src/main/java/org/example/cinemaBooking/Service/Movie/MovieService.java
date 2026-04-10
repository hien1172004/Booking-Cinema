package org.example.cinemaBooking.Service.Movie;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Response.Movie.MovieRecommendResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MovieStats;
import org.example.cinemaBooking.Entity.Category;

import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MovieMapper;
import org.example.cinemaBooking.DTO.Request.Movie.CreateMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieStatusRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieResponse;
import org.example.cinemaBooking.Repository.CategoryRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.spefication.MovieSpecification;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.AgeRating;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)

public class MovieService {
    MovieRepository movieRepository;
    CategoryRepository categoryRepository;
    MovieMapper movieMapper;



    public MovieResponse creatMovie(CreateMovieRequest request) {

        Optional<Movie> existingMovie = movieRepository.findBySlug(request.getSlug());

        if(existingMovie.isPresent()){
            throw new AppException(ErrorCode.MOVIE_SLUG_ALREADY_EXISTS);
        }

        Movie movie = movieMapper.toMovie(request);
        movie.setAgeRating(AgeRating.valueOf(request.getAgeRating()));

        Set<Category> categories =
                new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));

        if (categories.size() != request.getCategoryIds().size()) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        movie.setCategories(categories);
        Movie savedMovie =  movieRepository.save(movie);
        log.info("[MOVIE SERVICE] Movie {} has been created", savedMovie.getId());
        return movieMapper.toMovieResponse(savedMovie);
    }


    public MovieResponse updateMovie(String id, UpdateMovieRequest request) {
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        if (request.getSlug() != null) {
            Optional<Movie> existing = movieRepository.findBySlug(request.getSlug());

            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                // 👉 nếu movie khác có cùng slug
                throw new AppException(ErrorCode.MOVIE_SLUG_ALREADY_EXISTS);
            }
        }


        movieMapper.updateMovie(movie, request);

        if (request.getAgeRating() != null) {
            movie.setAgeRating(AgeRating.valueOf(request.getAgeRating()));
        }
        if (request.getCategoryIds() != null) {
            Set<Category> categories =
                    new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));

            movie.setCategories(categories);
            if (categories.size() != request.getCategoryIds().size()) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] Movie {} has been updated", movie.getId());
        return movieMapper.toMovieResponse(movie);
    }

    public void deleteMovie(String id){
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        movie.softDelete();
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] Movie {} has been deleted", movie.getId());
    }

    public MovieResponse getMovieById(String id) {
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        return movieMapper.toMovieResponse(movie);
    }

    public MovieResponse getMovieBySlug(String slug) {
        Movie movie = movieRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        return movieMapper.toMovieResponse(movie);
    }

    public PageResponse<MovieResponse> getMovies(
            String keyword,
            MovieStatus status,
            String categoryId,
            AgeRating ageRating,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        Specification<Movie> spec =
                MovieSpecification.filterMovie(keyword, status, categoryId, ageRating);

        Page<Movie> movies = movieRepository.findAll(spec, pageable);

        List<MovieResponse> movieResponses = movies.getContent().stream()
                .map(movieMapper::toMovieResponse)
                .toList();
        return PageResponse.<MovieResponse>builder()
                .page(page)
                .size(size)
                .totalElements(movies.getTotalElements())
                .totalPages(movies.getTotalPages())
                .items(movieResponses)
                .build();
    }

    public MovieResponse updateMovieStatus(String id, UpdateMovieStatusRequest request) {
        MovieStatus status = MovieStatus.valueOf(request.getStatus());
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        movie.setStatus(status);
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] Movie {} status has been updated to {}", movie.getId(), status);
        return movieMapper.toMovieResponse(movie);
    }

    public PageResponse<MovieResponse> getMoviesIsComingSoon(int page, int size) {
        int pageNumber = 0;
        if(page > 0) {
            pageNumber = page - 1;
        }
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("releaseDate").descending());

        Page<Movie> movies = movieRepository.findByStatusAndDeletedFalse(MovieStatus.COMING_SOON, pageable);

        List<MovieResponse> movieResponses = movies.getContent().stream()
                .map(movieMapper::toMovieResponse)
                .toList();
        return PageResponse.<MovieResponse>builder()
                .page(page)
                .size(size)
                .totalElements(movies.getTotalElements())
                .totalPages(movies.getTotalPages())
                .items(movieResponses)
                .build();
    }

    public PageResponse<MovieResponse> getMoviesIsNowShowing(int page, int size) {
        int pageNumber = 0;
        if(page > 0) {
            pageNumber = page - 1;
        }
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("releaseDate").descending());

        Page<Movie> movies = movieRepository.findByStatusAndDeletedFalse(MovieStatus.NOW_SHOWING, pageable);

        List<MovieResponse> movieResponses = movies.getContent().stream()
                .map(movieMapper::toMovieResponse)
                .toList();
        return PageResponse.<MovieResponse>builder()
                .page(page)
                .size(size)
                .totalElements(movies.getTotalElements())
                .totalPages(movies.getTotalPages())
                .items(movieResponses)
                .build();
    }

    public PageResponse<MovieResponse> searchMovie(int page, int size, String key) {
        int pageNumber = 0;
        if(page > 0) {
            pageNumber = page - 1;
        }
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("releaseDate").descending());

        Page<Movie> movies = movieRepository.searchMovie(key, pageable);

        List<MovieResponse> movieResponses = movies.getContent().stream()
                .map(movieMapper::toMovieResponse)
                .toList();
        return PageResponse.<MovieResponse>builder()
                .page(page)
                .size(size)
                .totalElements(movies.getTotalElements())
                .totalPages(movies.getTotalPages())
                .items(movieResponses)
                .build();
    }

    public List<MovieRecommendResponse> recommend() {
        List<MovieStats> stats = movieRepository.getMovieStats();

        double currentMaxRev = stats.stream().mapToDouble(MovieStats::revenue).max().orElse(1);
        double maxRevenue = currentMaxRev > 0 ? currentMaxRev : 1; // Tránh lỗi chia cho 0 tạo ra NaN

        long currentMaxTic = stats.stream().mapToLong(MovieStats::ticketCount).max().orElse(1);
        double maxTicket = currentMaxTic > 0 ? currentMaxTic : 1; // Tránh lỗi chia cho 0 tạo ra NaN

        return stats.stream()
                .map(m -> {
                    double revenueNorm = m.revenue() / maxRevenue;
                    double ticketNorm = m.ticketCount() / maxTicket;
                    double ratingNorm = m.rating() / 10.0; // vì bạn dùng 1-10

                    double score =
                            revenueNorm * 0.5 +
                            ticketNorm * 0.3 +
                            ratingNorm * 0.2;

                    return new MovieRecommendResponse(
                            m.id(),
                            m.title(),
                            m.posterUrl(),
                            score
                    );
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(10)
                .toList();
    }

    // chạy mỗi ngày lúc 00:00
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateMovieStatus() {

        LocalDate today = LocalDate.now();

        int updated = movieRepository.updateStatusToNowShowing(
                MovieStatus.NOW_SHOWING,
                MovieStatus.COMING_SOON,
                today
        );

        log.info("[SCHEDULER] Updated {} movies to NOW_SHOWING", updated);
    }

}
