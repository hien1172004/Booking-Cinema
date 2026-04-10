package org.example.cinemaBooking.Service.AI;

import lombok.RequiredArgsConstructor;
import org.example.cinemaBooking.DTO.Response.Movie.MovieRecommendResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MovieResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.SeatMapResponse;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSummaryResponse;
import org.example.cinemaBooking.Service.Booking.BookingService;
import org.example.cinemaBooking.Service.Movie.MovieService;
import org.example.cinemaBooking.Service.Showtime.ShowTimeSeatService;
import org.example.cinemaBooking.Service.Showtime.ShowtimeService;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CinemaChatTools {

    private final MovieService movieService;
    private final ShowtimeService showtimeService;
    private final ShowTimeSeatService showTimeSeatService;
    private final BookingService bookingService;

    @Tool(description = """
        Tìm kiếm phim đang chiếu hoặc sắp chiếu.
        Dùng khi user hỏi: 'phim gì đang chiếu', 'có phim gì hay', 'tìm phim X'.
        """)
    public PageResponse<MovieResponse> searchMovies(
            @ToolParam(description = "Từ khóa tìm kiếm tên phim, để trống để lấy tất cả") String keyword,
            @ToolParam(description = "Trạng thái: NOW_SHOWING hoặc COMING_SOON, để trống nếu không lọc") String status) {

        MovieStatus movieStatus = null;
        if (status != null && !status.isBlank()) {
            try { movieStatus = MovieStatus.valueOf(status); } catch (Exception ignored) {}
        }
        return movieService.getMovies(keyword, movieStatus, null, null, 1, 10, "releaseDate", "desc");
    }

    @Tool(description = """
        Lấy danh sách phim đang chiếu hiện tại (NOW_SHOWING).
        Dùng khi user hỏi 'phim đang chiếu', 'chiếu gì hôm nay'.
        """)
    public PageResponse<MovieResponse> getNowShowingMovies(
            @ToolParam(description = "Số trang, mặc định 1") int page) {
        return movieService.getMoviesIsNowShowing(page > 0 ? page : 1, 8);
    }

    @Tool(description = """
        Lấy danh sách phim sắp chiếu (COMING_SOON).
        Dùng khi user hỏi 'phim sắp ra', 'sắp có phim gì mới'.
        """)
    public PageResponse<MovieResponse> getComingSoonMovies(
            @ToolParam(description = "Số trang, mặc định 1") int page) {
        return movieService.getMoviesIsComingSoon(page > 0 ? page : 1, 8);
    }

    @Tool(description = """
        Lấy chi tiết một bộ phim theo slug (đường dẫn thân thiện).
        Dùng khi user hỏi thông tin cụ thể về phim như nội dung, đạo diễn, thể loại.
        Ví dụ slug: 'avengers-endgame', 'spider-man-no-way-home'.
        """)
    public MovieResponse getMovieDetail(
            @ToolParam(description = "Slug của phim, ví dụ: avengers-endgame") String slug) {
        return movieService.getMovieBySlug(slug);
    }

    @Tool(description = """
        Lấy lịch chiếu của một bộ phim theo ngày.
        Dùng khi user hỏi 'lịch chiếu phim X', 'phim X chiếu mấy giờ', 'suất chiếu hôm nay'.
        """)
    public List<ShowtimeSummaryResponse> getShowtimesByMovieAndDate(
            @ToolParam(description = "ID của phim") String movieId,
            @ToolParam(description = "Ngày chiếu dạng yyyy-MM-dd, ví dụ: 2025-06-15") String date) {
        return showtimeService.getShowtimeByMovieAndDate(movieId, parseDate(date));
    }

    @Tool(description = """
        Lấy lịch chiếu theo rạp và ngày.
        Dùng khi user hỏi 'rạp X hôm nay chiếu gì', 'lịch chiếu tại rạp Y'.
        """)
    public List<ShowtimeSummaryResponse> getShowtimesByCinemaAndDate(
            @ToolParam(description = "ID của rạp chiếu phim") String cinemaId,
            @ToolParam(description = "Ngày chiếu dạng yyyy-MM-dd") String date) {
        return showtimeService.getShowtimesByCinemaAndDate(cinemaId, parseDate(date));
    }

    @Tool(description = """
        Xem sơ đồ ghế của một suất chiếu: ghế nào còn trống, ghế nào đã đặt.
        Dùng khi user hỏi 'còn ghế không', 'ghế nào còn trống', 'chọn ghế'.
        """)
    public SeatMapResponse getSeatMap(
            @ToolParam(description = "ID của suất chiếu") String showtimeId) {
        return showTimeSeatService.getSeatMap(showtimeId);
    }

    @Tool(description = """
        Gợi ý 10 phim nên xem dựa trên doanh thu, số vé bán và đánh giá.
        Dùng khi user hỏi 'phim nào hay nhất', 'gợi ý phim cho tôi', 'phim hot nhất'.
        """)
    public List<MovieRecommendResponse> getRecommendedMovies() {
        return movieService.recommend();
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày không đúng định dạng yyyy-MM-dd");
        }
    }
}