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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    /**
     * Tạo mới một bộ phim.
     * <p>Xóa tất cả các cache liên quan đến danh sách phim do có phim mới được thêm vào.</p>
     *
     * @param request Dữ liệu phim cần tạo
     * @return MovieResponse thông tin phim vừa tạo
     */
    @Caching(evict = {
            @CacheEvict(value = "movies", allEntries = true),
            @CacheEvict(value = "movies-coming-soon", allEntries = true),
            @CacheEvict(value = "movies-now-showing", allEntries = true),
            @CacheEvict(value = "movies-search", allEntries = true)
    })
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

    /**
     * Cập nhật thông tin phim.
     * <p>Xóa cache danh sách chung, chi tiết phim và các danh sách phân loại trạng thái.</p>
     *
     * @param id      ID phim cần cập nhật
     * @param request Dữ liệu phim cập nhật
     * @return MovieResponse thông tin phim sau khi cập nhật
     */
    @Caching(evict = {
            @CacheEvict(value = "movies", allEntries = true),
            @CacheEvict(value = "movies-coming-soon", allEntries = true),
            @CacheEvict(value = "movies-now-showing", allEntries = true),
            @CacheEvict(value = "movies-search", allEntries = true),
            @CacheEvict(value = "movie", key = "#id"),
            @CacheEvict(value = "movie-slug", key = "#request.slug")
    })
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

    /**
     * Xóa bộ phim (Soft Delete).
     * <p>Xóa các cache chứa thông tin phim và danh sách phim.</p>
     *
     * @param id ID của phim cần xóa
     */
    @Caching(evict = {
            @CacheEvict(value = "movies", allEntries = true),
            @CacheEvict(value = "movies-coming-soon", allEntries = true),
            @CacheEvict(value = "movies-now-showing", allEntries = true),
            @CacheEvict(value = "movies-search", allEntries = true),
            @CacheEvict(value = "movie", key = "#id"),
            @CacheEvict(value = "movie-slug", allEntries = true)
    })
    public void deleteMovie(String id){
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        movie.softDelete();
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] Movie {} has been deleted", movie.getId());
    }

    /**
     * Lấy thông tin chi tiết phim theo ID.
     * <p>Kết quả được lưu vào cache "movie".</p>
     *
     * @param id ID của phim
     * @return MovieResponse thông tin chi tiết
     */
    @Cacheable(value = "movie", key = "#id")
    public MovieResponse getMovieById(String id) {
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        return movieMapper.toMovieResponse(movie);
    }

    /**
     * Lấy thông tin chi tiết phim theo Slug.
     * <p>Kết quả được lưu vào cache "movie-slug" với key là slug.</p>
     *
     * @param slug Đường dẫn không dấu của phim
     * @return MovieResponse thông tin chi tiết
     */
    @Cacheable(value = "movie-slug", key = "#slug")
    public MovieResponse getMovieBySlug(String slug) {
        Movie movie = movieRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        return movieMapper.toMovieResponse(movie);
    }

    /**
     * Lấy danh sách phim (kèm lọc, sắp xếp, và phân trang).
     * <p>Được thiết lập cache với các key tổng hợp.</p>
     *
     * @param keyword    Từ khoá tìm kiếm
     * @param status     Trạng thái phim
     * @param categoryId Thể loại phim
     * @param ageRating  Xếp hạng độ tuổi
     * @param page       Số thứ tự trang
     * @param size       Kích thước trang
     * @param sortBy     Trường sắp xếp
     * @param sortDir    Hướng sắp xếp
     * @return PageResponse các phim thoả mãn yêu cầu
     */
    @Cacheable(value = "movies",
            key = "(#keyword ?: '') + '-' + (#status != null ? #status.name() : '') + '-' + (#categoryId ?: '') + '-' + (#ageRating != null ? #ageRating.name() : '') + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
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

    /**
     * Cập nhật trạng thái một bộ phim (ví dụ: đang chiếu, ngừng chiếu...).
     * <p>Đồng thời tái tạo cache tránh trình trạng người dùng thấy dữ liệu cũ.</p>
     *
     * @param id      ID phim
     * @param request Yêu cầu chứa trạng thái mới
     * @return MovieResponse thông tin sau khi nhận cập nhật
     */
    @Caching(evict = {
            @CacheEvict(value = "movies", allEntries = true),
            @CacheEvict(value = "movies-coming-soon", allEntries = true),
            @CacheEvict(value = "movies-now-showing", allEntries = true),
            @CacheEvict(value = "movies-search", allEntries = true),
            @CacheEvict(value = "movie", key = "#id"),
            @CacheEvict(value = "movie-slug", allEntries = true)
    })
    public MovieResponse updateMovieStatus(String id, UpdateMovieStatusRequest request) {
        MovieStatus status = MovieStatus.valueOf(request.getStatus());
        Movie movie = movieRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
        movie.setStatus(status);
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] Movie {} status has been updated to {}", movie.getId(), status);
        return movieMapper.toMovieResponse(movie);
    }

    /**
     * Lấy danh sách phim "Sắp chiếu".
     * <p>Cache tại "movies-coming-soon" để tải dữ liệu ở trang màn hình Home nhanh chóng.</p>
     *
     * @param page Số trang
     * @param size Kích thước trang
     * @return PageResponse phim sắp chiếu
     */
    @Cacheable(value = "movies-coming-soon", key = "#page + '-' + #size")
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

    /**
     * Lấy danh sách phim "Đang chiếu".
     * <p>Cache tại "movies-now-showing" để tải dữ liệu ở trang màn hình Home siêu nhanh.</p>
     *
     * @param page Số trang
     * @param size Kích thước trang
     * @return PageResponse phim đang chiếu
     */
    @Cacheable(value = "movies-now-showing", key = "#page + '-' + #size")
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

    /**
     * Tìm kiếm phim nhanh chóng theo từ khoá chung.
     * <p>Kết quả tìm kiếm cache tại "movies-search".</p>
     *
     * @param page Số trang
     * @param size Kích thước trang
     * @param key  Từ khoá theo title, mô tả
     * @return PageResponse danh sách tìm kiếm
     */
    @Cacheable(value = "movies-search", key = "#page + '-' + #size + '-' + (#key ?: '')")
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
    @Caching(evict = {
            @CacheEvict(value = "movies", allEntries = true),
            @CacheEvict(value = "movies-coming-soon", allEntries = true),
            @CacheEvict(value = "movies-now-showing", allEntries = true),
            @CacheEvict(value = "movies-search", allEntries = true),
            @CacheEvict(value = "movie", allEntries = true),
            @CacheEvict(value = "movie-slug", allEntries = true)
    })
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
