package org.example.cinemaBooking.Service.Chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Mapper.MovieMapper;
import org.example.cinemaBooking.Repository.*;
import org.example.cinemaBooking.Service.Movie.MovieService;
import org.example.cinemaBooking.Shared.enums.DiscountType;
import org.example.cinemaBooking.Shared.enums.MovieStatus;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.SeatTypeEnum;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tập hợp các Tool mà AI Chatbot có thể gọi để tra cứu dữ liệu thực tế
 * từ hệ thống Cinema Booking.
 * <p>
 * Quy tắc: Tất cả Tool chỉ đọc (READ-ONLY), không thực hiện đặt vé/thanh toán.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotTools {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieService movieService;
    private final MovieMapper movieMapper;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final ComboRepository comboRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 1 — Phim đang chiếu
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Lấy danh sách các phim đang được chiếu tại rạp hiện tại.
            Dùng khi khách hỏi: 'Hôm nay có phim gì?', 'Phim đang chiếu là gì?',
            'Rạp đang chiếu phim nào?'
            """)
    @Transactional(readOnly = true)
    public String getNowShowingMovies() {
        log.info("[CHATBOT TOOL] getNowShowingMovies called");
        List<Movie> movies = movieRepository.findByStatusAndDeletedFalse(
                        MovieStatus.NOW_SHOWING, PageRequest.of(0, 10, Sort.by("releaseDate").descending()))
                .getContent();

        if (movies.isEmpty())
            return "Hiện tại rạp chưa có phim đang chiếu.";

        return String.format("Danh sách phim đang chiếu:%n") + movies.stream()
                .map(m -> String.format("• **%s** (%d phút) - Khởi chiếu: %s",
                        m.getTitle(), m.getDuration(),
                        m.getReleaseDate() != null ? m.getReleaseDate().format(DATE_FMT)
                                : "N/A"))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 2 — Phim sắp chiếu
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Lấy danh sách các phim sắp ra mắt / sắp chiếu trong thời gian tới.
            Dùng khi khách hỏi: 'Sắp có phim gì?', 'Phim nào sắp ra rạp?',
            'Phim mới sắp chiếu?'
            """)
    @Transactional(readOnly = true)
    public String getComingSoonMovies() {
        log.info("[CHATBOT TOOL] getComingSoonMovies called");
        List<Movie> movies = movieRepository.findByStatusAndDeletedFalse(
                        MovieStatus.COMING_SOON, PageRequest.of(0, 10, Sort.by("releaseDate").ascending()))
                .getContent();

        if (movies.isEmpty())
            return "Hiện tại rạp chưa có thông tin phim sắp chiếu.";

        return String.format("Danh sách phim sắp chiếu:%n") + movies.stream()
                .map(m -> String.format("• **%s** - Dự kiến chiếu: %s",
                        m.getTitle(),
                        m.getReleaseDate() != null ? m.getReleaseDate().format(DATE_FMT)
                                : "Chưa xác định"))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 3 — Gợi ý phim hay
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Gợi ý những bộ phim được đánh giá cao và đang có doanh thu tốt nhất.
            Dùng khi khách hỏi: 'Phim nào hay?', 'Gợi ý phim cho tôi',
            'Phim nào đang hot?', 'Phim nào đáng xem nhất?'
            """)
    @Transactional(readOnly = true)
    public String getRecommendedMovies() {
        log.info("[CHATBOT TOOL] getRecommendedMovies called");
        var recommendations = movieService.recommend();
        if (recommendations.isEmpty())
            return "Hiện chưa có đủ dữ liệu để gợi ý phim.";

        return String.format("Top phim được gợi ý:%n") + recommendations.stream()
                .limit(5)
                .map(r -> String.format("• **%s** (Điểm: %.2f/1.00)", r.title(), r.score()))
                .collect(Collectors.joining(System.lineSeparator()))
                + String.format("%n%nBạn có muốn xem lịch chiếu của phim nào không?");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 4 — Tìm phim theo thể loại
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Tìm phim đang chiếu hoặc sắp chiếu theo thể loại / genre.
            Dùng khi khách hỏi: 'Có phim hành động không?', 'Phim kinh dị',
            'Tôi muốn xem phim tình cảm', 'Phim hoạt hình cho trẻ em', ...
            Tham số genreName: tên thể loại phim (ví dụ: hành động, kinh dị, lãng mạn).
            """)
    @Transactional(readOnly = true)
    public String getMoviesByGenre(String genreName) {
        log.info("[CHATBOT TOOL] getMoviesByGenre called with genre={}", genreName);
        List<Movie> movies = movieRepository.findByGenreName(genreName);

        if (movies.isEmpty()) {
            return String.format(
                    "Hiện tại rạp không có phim thuộc thể loại '%s' đang chiếu hoặc sắp chiếu.",
                    genreName);
        }

        return String.format("Phim thuộc thể loại **%s**:%n", genreName) +
                movies.stream()
                        .limit(8)
                        .map(m -> String.format("• **%s** (%s) - %d phút",
                                m.getTitle(), m.getStatus().name().replace("_", " "),
                                m.getDuration()))
                        .collect(Collectors.joining(System.lineSeparator()));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 5 — Tìm phim theo diễn viên / đạo diễn
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Tìm phim có sự tham gia của diễn viên hoặc đạo diễn theo tên.
            Dùng khi khách hỏi: 'Phim nào có [tên diễn viên]?',
            'Phim do [đạo diễn] đạo diễn?', 'Phim của diễn viên X đang chiếu không?'
            Tham số actorName: tên diễn viên hoặc đạo diễn cần tìm.
            """)
    @Transactional(readOnly = true)
    public String getMoviesByActor(String actorName) {
        log.info("[CHATBOT TOOL] getMoviesByActor called with actor={}", actorName);
        List<Movie> movies = movieRepository.findByActorName(actorName);

        if (movies.isEmpty()) {
            return String.format("Hiện tại rạp không có phim nào của '%s' đang chiếu hoặc sắp chiếu.",
                    actorName);
        }

        return String.format("Phim có sự tham gia của **%s**:%n", actorName) +
                movies.stream()
                        .limit(8)
                        .map(m -> String.format("• **%s** (%s)",
                                m.getTitle(), m.getStatus().name().replace("_", " ")))
                        .collect(Collectors.joining(System.lineSeparator()));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 6 — Lịch chiếu theo tên phim (ngày hôm nay)
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Tra cứu lịch chiếu của một bộ phim cụ thể trong ngày hôm nay.
            Dùng khi khách hỏi: 'Phim X chiếu lúc mấy giờ?', 'Lịch chiếu của phim Y hôm nay?',
            'Còn suất chiếu nào tối nay không?'
            Tham số movieTitle: tên phim cần tra cứu lịch chiếu (không cần chính xác tuyệt đối).
            """)
    @Transactional(readOnly = true)
    public String getShowtimesByMovieTitle(String movieTitle) {
        log.info("[CHATBOT TOOL] getShowtimesByMovieTitle called with movie={}", movieTitle);

        // Tìm phim theo từ khóa tên
        var moviePage = movieRepository.searchMovie(movieTitle, PageRequest.of(0, 1));
        if (moviePage.isEmpty()) {
            return String.format("Không tìm thấy phim nào có tên '%s' đang chiếu.", movieTitle);
        }

        Movie movie = moviePage.getContent().get(0);
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        List<Showtime> showtimes = showtimeRepository.findByMovieAndDateRange(
                movie.getId(), from, to, ShowTimeStatus.CANCELLED);

        if (showtimes.isEmpty()) {
            return String.format("Phim **%s** không có suất chiếu nào trong hôm nay.", movie.getTitle());
        }

        String schedule = showtimes.stream()
                .map(s -> String.format("  ⏰ %s | Rạp: %s | Phòng: %s | Còn %d ghế",
                        s.getStartTime().format(DATETIME_FMT),
                        s.getRoom().getCinema().getName(),
                        s.getRoom().getName(),
                        s.getAvailableSeats()))
                .collect(Collectors.joining(System.lineSeparator()));

        return String.format("Lịch chiếu của **%s** hôm nay (%s):%n%s",
                movie.getTitle(), today.format(DATE_FMT), schedule);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 7 — Tra cứu trạng thái vé theo booking code
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Tra cứu trạng thái của một đơn đặt vé theo mã booking.
            Dùng khi khách hỏi: 'Đơn vé BK... của tôi ra sao?', 'Mã đặt vé của tôi là ...',
            'Kiểm tra trạng thái vé số ...', 'Vé của tôi đã thanh toán chưa?'
            Tham số bookingCode: mã đặt vé (bắt đầu bằng BK).
            """)
    @Transactional(readOnly = true)
    public String checkBookingStatus(String bookingCode) {
        log.info("[CHATBOT TOOL] checkBookingStatus called with code={}", bookingCode);

        Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode.trim().toUpperCase());

        if (bookingOpt.isEmpty()) {
            return String.format("Không tìm thấy đơn đặt vé nào với mã **%s**. Vui lòng kiểm tra lại.",
                    bookingCode);
        }

        Booking booking = bookingOpt.get();
        String status = switch (booking.getStatus()) {
            case PENDING -> "⏳ Đang chờ thanh toán";
            case CONFIRMED -> "✅ Đã xác nhận / Đã thanh toán";
            case CANCELLED -> "❌ Đã hủy";
            case EXPIRED -> "⏰ Đã hết hạn (quá thời gian thanh toán)";
            case REFUNDED -> "♻️ Đã hoàn tiền";
        };

        return String.format("Thông tin đơn đặt vé **%s**:%n" +
                        "• Trạng thái: %s%n" +
                        "• Phim: %s%n" +
                        "• Suất chiếu: %s%n" +
                        "• Số vé: %d%n" +
                        "• Tổng tiền: %,.0f VNĐ%n",
                booking.getBookingCode(),
                status,
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getStartTime().format(DATETIME_FMT),
                booking.getTickets().size(),
                booking.getFinalPrice().doubleValue());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 8 — Thông tin liên hệ rạp
    // ────────────────────────────────────────────────────────────────────────────

    @Tool(description = """
            Lấy thông tin địa chỉ, hotline và số điện thoại của tất cả các rạp trong hệ thống.
            Dùng khi khách hỏi: 'Địa chỉ rạp ở đâu?', 'Số điện thoại rạp?',
            'Hotline hỗ trợ?', 'Rạp gần nhất ở đâu?', 'Liên hệ rạp như thế nào?'
            """)
    @Transactional(readOnly = true)
    public String getCinemaContactInfo() {
        log.info("[CHATBOT TOOL] getCinemaContactInfo called");
        List<Cinema> cinemas = cinemaRepository.findAll();

        if (cinemas.isEmpty())
            return "Hiện tại chưa có thông tin rạp trong hệ thống.";

        return String.format("Thông tin các rạp:%n") + cinemas.stream()
                .map(c -> String.format("• **%s**%n  📍 %s%n  📞 %s / Hotline: %s",
                        c.getName(),
                        c.getAddress() != null ? c.getAddress() : "Chưa cập nhật",
                        c.getPhone() != null ? c.getPhone() : "N/A",
                        c.getHotline() != null ? c.getHotline() : "N/A"))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 9 — Suất chiếu của phim trong ngày cụ thể
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Tra cứu suất chiếu của một bộ phim cụ thể trong một ngày cụ thể (không nhất thiết là hôm nay).
            Dùng khi khách hỏi: 'Phim X chiếu ngày mai lúc mấy giờ?',
            'Lịch chiếu phim Y ngày 20/04?', 'Cuối tuần có suất chiếu phim Z không?'
            Tham số movieTitle: tên phim cần tra cứu.
            Tham số date: ngày cần tra cứu, định dạng dd/MM/yyyy (ví dụ: 15/04/2026).
            """)
    @Transactional(readOnly = true)
    public String getShowtimesByMovieAndDate(String movieTitle, String date) {
        log.info("[CHATBOT TOOL] getShowtimesByMovieAndDate called with movie={}, date={}", movieTitle, date);
        LocalDate targetDate = parseDate(date);
        if (targetDate == null) {
            return "Ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy (ví dụ: 15/04/2026).";
        }
        var moviePage = movieRepository.searchMovie(movieTitle, PageRequest.of(0, 1));
        if (moviePage.isEmpty()) {
            return String.format("Không tìm thấy phim nào có tên '%s'.", movieTitle);
        }
        Movie movie = moviePage.getContent().get(0);
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        List<Showtime> showtimes = showtimeRepository.findByMovieAndDateRange(
                movie.getId(), from, to, ShowTimeStatus.CANCELLED);
        if (showtimes.isEmpty()) {
            return String.format("Phim **%s** không có suất chiếu nào vào ngày %s.",
                    movie.getTitle(), targetDate.format(DATE_FMT));
        }
        String schedule = showtimes.stream()
                .map(s -> String.format("  ⏰ %s | Rạp: %s | Phòng: %s | Còn %d ghế",
                        s.getStartTime().format(DATETIME_FMT),
                        s.getRoom().getCinema().getName(),
                        s.getRoom().getName(),
                        s.getAvailableSeats()))
                .collect(Collectors.joining(System.lineSeparator()));
        return String.format("Lịch chiếu của **%s** ngày %s:%n%s",
                movie.getTitle(), targetDate.format(DATE_FMT), schedule);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 10 — Tìm suất chiếu theo rạp + ngày
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Tra cứu tất cả suất chiếu của một rạp cụ thể trong một ngày nào đó.
            Dùng khi khách hỏi: 'Rạp X hôm nay có phim gì chiếu?',
            'Lịch chiếu rạp Y ngày mai?', 'Rạp Z cuối tuần có gì?',
            'Hôm nay rạp A chiếu phim gì?'
            Tham số cinemaName: tên rạp cần tra cứu (không cần chính xác tuyệt đối).
            Tham số date: ngày cần tra, định dạng dd/MM/yyyy. Nếu hỏi hôm nay thì truyền ngày hôm nay.
            """)
    @Transactional(readOnly = true)
    public String getShowtimesByCinemaAndDate(String cinemaName, String date) {
        log.info("[CHATBOT TOOL] getShowtimesByCinemaAndDate called with cinema={}, date={}", cinemaName, date);
        LocalDate targetDate = parseDate(date);
        if (targetDate == null) {
            return "Ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy (ví dụ: 15/04/2026).";
        }
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        List<Showtime> showtimes = showtimeRepository.findByCinemaNameAndDateRange(
                cinemaName, from, to, ShowTimeStatus.CANCELLED);
        if (showtimes.isEmpty()) {
            return String.format("Rạp '%s' không có suất chiếu nào vào ngày %s.",
                    cinemaName, targetDate.format(DATE_FMT));
        }
        // Gom suất chiếu theo tên phim
        Map<String, List<Showtime>> grouped = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getMovie().getTitle()));
        String cinemaActualName = showtimes.get(0).getRoom().getCinema().getName();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Lịch chiếu tại **%s** ngày %s:%n", cinemaActualName,
                targetDate.format(DATE_FMT)));
        grouped.forEach((movieTitle, sts) -> {
            sb.append(String.format("%n🎬 **%s**:%n", movieTitle));
            sts.forEach(s -> sb.append(String.format("  ⏰ %s | Phòng: %s | Còn %d ghế%n",
                    s.getStartTime().format(DATETIME_FMT),
                    s.getRoom().getName(),
                    s.getAvailableSeats())));
        });
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 11 — Tra cứu giá vé
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Tra cứu bảng giá vé xem phim, bao gồm giá cơ bản và phụ phí theo loại ghế.
            Dùng khi khách hỏi: 'Giá vé bao nhiêu?', 'Vé VIP giá bao nhiêu?',
            'Ghế đôi (couple) giá bao nhiêu?', 'Bảng giá vé xem phim?',
            'Giá vé phim X?', 'Phụ phí ghế VIP là bao nhiêu?'
            Tham số movieTitle: tên phim muốn tra giá (không bắt buộc, có thể null).
            """)
    @Transactional(readOnly = true)
    public String getTicketPriceInfo(String movieTitle) {
        log.info("[CHATBOT TOOL] getTicketPriceInfo called with movie={}", movieTitle);
        // Lấy bảng phụ phí loại ghế
        List<SeatType> seatTypes = seatTypeRepository.findAllByDeletedAtIsNull();
        StringBuilder sb = new StringBuilder();
        // Nếu có tên phim → tìm giá cơ bản từ suất chiếu gần nhất
        if (movieTitle != null && !movieTitle.isBlank()) {
            var moviePage = movieRepository.searchMovie(movieTitle, PageRequest.of(0, 1));
            if (!moviePage.isEmpty()) {
                Movie movie = moviePage.getContent().get(0);
                LocalDateTime from = LocalDate.now().atStartOfDay();
                LocalDateTime to = from.plusDays(7); // tìm trong 7 ngày tới
                List<Showtime> showtimes = showtimeRepository.findByMovieAndDateRange(
                        movie.getId(), from, to, ShowTimeStatus.CANCELLED);
                if (!showtimes.isEmpty()) {
                    Showtime nearestShowtime = showtimes.get(0);
                    sb.append(String.format("💰 Giá vé phim **%s**:%n", movie.getTitle()));
                    sb.append(String.format("• Giá cơ bản: **%,.0f VNĐ**%n",
                            nearestShowtime.getBasePrice().doubleValue()));
                    sb.append(String.format("%n📋 Phụ phí theo loại ghế:%n"));
                } else {
                    sb.append(String.format("Phim **%s** chưa có suất chiếu trong 7 ngày tới.%n",
                            movie.getTitle()));
                    sb.append(String.format("%n📋 Bảng phụ phí loại ghế (áp dụng chung):%n"));
                }
            } else {
                sb.append(String.format("Không tìm thấy phim '%s'.%n", movieTitle));
                sb.append(String.format("%n📋 Bảng phụ phí loại ghế (áp dụng chung):%n"));
            }
        } else {
            sb.append(String.format("📋 Bảng phụ phí theo loại ghế (cộng thêm vào giá cơ bản của suất chiếu):%n"));
        }
        if (seatTypes.isEmpty()) {
            sb.append("Chưa có thông tin loại ghế trong hệ thống.");
        } else {
            seatTypes.forEach(st -> {
                String typeName = switch (st.getName()) {
                    case STANDARD -> "🪑 Ghế thường (Standard)";
                    case VIP -> "⭐ Ghế VIP";
                    case COUPLE -> "💑 Ghế đôi (Couple)";
                };
                sb.append(String.format("  %s: +%,.0f VNĐ%n", typeName,
                        st.getPriceModifier().doubleValue()));
            });
        }
        sb.append(String.format("%n💡 **Giá vé = Giá cơ bản suất chiếu + Phụ phí loại ghế**"));
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 12 — Kiểm tra ghế trống theo suất chiếu
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Kiểm tra tình trạng ghế trống (ghế còn trống / đã đặt) của một suất chiếu cụ thể.
            Dùng khi khách hỏi: 'Suất chiếu X còn ghế không?', 'Ghế VIP còn trống không?',
            'Còn bao nhiêu ghế trống?', 'Kiểm tra ghế phim X suất lúc Y giờ',
            'Còn ghế đôi không?'
            Tham số movieTitle: tên phim muốn kiểm tra.
            Tham số date: ngày chiếu, định dạng dd/MM/yyyy.
            Tham số time: giờ chiếu muốn kiểm tra (ví dụ: '19:00'). Nếu không biết giờ, truyền null.
            """)
    @Transactional(readOnly = true)
    public String checkAvailableSeats(String movieTitle, String date, String time) {
        log.info("[CHATBOT TOOL] checkAvailableSeats called with movie={}, date={}, time={}", movieTitle, date,
                time);
        LocalDate targetDate = parseDate(date);
        if (targetDate == null) {
            return "Ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.";
        }
        var moviePage = movieRepository.searchMovie(movieTitle, PageRequest.of(0, 1));
        if (moviePage.isEmpty()) {
            return String.format("Không tìm thấy phim nào có tên '%s'.", movieTitle);
        }
        Movie movie = moviePage.getContent().get(0);
        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        List<Showtime> showtimes = showtimeRepository.findByMovieAndDateRange(
                movie.getId(), from, to, ShowTimeStatus.CANCELLED);
        if (showtimes.isEmpty()) {
            return String.format("Phim **%s** không có suất chiếu nào vào ngày %s.",
                    movie.getTitle(), targetDate.format(DATE_FMT));
        }
        // Nếu có giờ cụ thể → tìm suất gần nhất với giờ đó
        Showtime targetShowtime;
        if (time != null && !time.isBlank()) {
            try {
                int targetHour = Integer.parseInt(time.split(":")[0]);
                int targetMinute = time.contains(":") ? Integer.parseInt(time.split(":")[1]) : 0;
                LocalDateTime targetTime = targetDate.atTime(targetHour, targetMinute);
                targetShowtime = showtimes.stream()
                        .min((a, b) -> Long.compare(
                                Math.abs(java.time.Duration
                                        .between(a.getStartTime(), targetTime)
                                        .toMinutes()),
                                Math.abs(java.time.Duration
                                        .between(b.getStartTime(), targetTime)
                                        .toMinutes())))
                        .orElse(showtimes.get(0));
            } catch (Exception e) {
                targetShowtime = showtimes.get(0);
            }
        } else {
            // Lấy suất chiếu đầu tiên chưa bắt đầu
            targetShowtime = showtimes.stream()
                    .filter(s -> s.getStartTime().isAfter(LocalDateTime.now()))
                    .findFirst()
                    .orElse(showtimes.get(0));
        }
        // Lấy chi tiết ghế
        List<ShowtimeSeat> seats = showtimeSeatRepository
                .findAllByShowtimeIdWithDetails(targetShowtime.getId());
        if (seats.isEmpty()) {
            return String.format("Chưa có thông tin ghế cho suất chiếu **%s** lúc %s.",
                    movie.getTitle(), targetShowtime.getStartTime().format(DATETIME_FMT));
        }
        // Đếm theo trạng thái + loại ghế
        long totalSeats = seats.size();
        long available = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long booked = seats.stream().filter(s -> s.getStatus() == SeatStatus.BOOKED).count();
        long locked = seats.stream().filter(s -> s.getStatus() == SeatStatus.LOCKED).count();
        // Đếm ghế trống theo loại
        Map<SeatTypeEnum, Long> availableByType = seats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .collect(Collectors.groupingBy(
                        s -> s.getSeat().getSeatType().getName(),
                        Collectors.counting()));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎬 Phim: **%s**%n", movie.getTitle()));
        sb.append(String.format("⏰ Suất: **%s**%n", targetShowtime.getStartTime().format(DATETIME_FMT)));
        sb.append(String.format("🏠 Rạp: %s | Phòng: %s%n%n",
                targetShowtime.getRoom().getCinema().getName(),
                targetShowtime.getRoom().getName()));
        sb.append(String.format("📊 Tổng quan: **%d/%d ghế trống**%n", available, totalSeats));
        sb.append(String.format("  ✅ Còn trống: %d | ❌ Đã đặt: %d | 🔒 Đang giữ: %d%n%n", available, booked,
                locked));
        sb.append(String.format("📋 Ghế trống theo loại:%n"));
        availableByType.forEach((type, count) -> {
            String typeName = switch (type) {
                case STANDARD -> "🪑 Ghế thường";
                case VIP -> "⭐ Ghế VIP";
                case COUPLE -> "💑 Ghế đôi";
            };
            sb.append(String.format("  %s: %d ghế trống%n", typeName, count));
        });
        if (available == 0) {
            sb.append(String.format("%n⚠️ Suất chiếu này đã hết ghế! Bạn có muốn xem suất chiếu khác không?"));
        }
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 13 — Phim đang chiếu tại rạp cụ thể
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Tra cứu danh sách phim đang chiếu hoặc có lịch chiếu tại một rạp cụ thể.
            Dùng khi khách hỏi: 'Rạp X có phim gì?', 'Phim nào đang chiếu ở rạp Y?',
            'Rạp Z có những phim nào?', 'Tôi muốn biết phim ở rạp A?'
            Tham số cinemaName: tên rạp cần tra cứu (không cần chính xác tuyệt đối).
            """)
    @Transactional(readOnly = true)
    public String getMoviesByCinema(String cinemaName) {
        log.info("[CHATBOT TOOL] getMoviesByCinema called with cinema={}", cinemaName);
        // Tìm rạp
        List<Cinema> cinemas = cinemaRepository.findByNameContainingIgnoreCaseAndDeletedFalse(cinemaName);
        if (cinemas.isEmpty()) {
            return String.format("Không tìm thấy rạp nào có tên '%s'. " +
                    "Bạn có thể hỏi 'Thông tin rạp' để xem danh sách tất cả các rạp.", cinemaName);
        }
        Cinema cinema = cinemas.get(0);
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = from.plusDays(7); // Tìm trong 7 ngày tới
        List<Showtime> showtimes = showtimeRepository.findByCinemaAndDateRange(
                cinema.getId(), from, to, ShowTimeStatus.CANCELLED);
        if (showtimes.isEmpty()) {
            return String.format("Rạp **%s** hiện chưa có lịch chiếu trong 7 ngày tới.", cinema.getName());
        }
        // Gom theo phim (không bị trùng)
        Map<String, Movie> uniqueMovies = showtimes.stream()
                .collect(Collectors.toMap(
                        s -> s.getMovie().getId(),
                        Showtime::getMovie,
                        (a, b) -> a));
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎬 Phim đang có lịch chiếu tại **%s**:%n%n", cinema.getName()));
        uniqueMovies.values().forEach(m -> sb.append(String.format("• **%s** (%d phút) - %s%n",
                m.getTitle(), m.getDuration(),
                m.getStatus().name().replace("_", " "))));
        sb.append(String.format("%nBạn muốn xem lịch chiếu cụ thể của phim nào?"));
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 14 — Combo đồ ăn / bắp nước
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Tra cứu danh sách combo đồ ăn, bắp nước, đồ uống có tại rạp.
            Dùng khi khách hỏi: 'Có combo gì?', 'Bắp nước giá bao nhiêu?',
            'Combo ăn kèm có gì?', 'Đồ ăn ở rạp?', 'Nước uống giá bao nhiêu?',
            'Menu bắp nước?', 'Mua bắp rang bơ bao nhiêu?'
            """)
    @Transactional(readOnly = true)
    public String getFoodAndDrinkCombos() {
        log.info("[CHATBOT TOOL] getFoodAndDrinkCombos called");

        var comboPage = comboRepository.findByActiveTrueAndDeletedFalse(PageRequest.of(0, 20));
        List<Combo> combos = comboPage.getContent();

        var productPage = productRepository.findByActiveTrueAndDeletedFalse(PageRequest.of(0, 20));
        List<Product> products = productPage.getContent();

        if (combos.isEmpty() && products.isEmpty()) {
            return "Hiện tại rạp chưa cập nhật menu đồ ăn / bắp nước.";
        }

        StringBuilder sb = new StringBuilder();

        // COMBO SECTION
        if (!combos.isEmpty()) {
            sb.append(String.format("🍿 **COMBO BẮP NƯỚC**:%n%n"));

            for (Combo c : combos) {
                sb.append(formatCombo(c)).append(System.lineSeparator());
            }
        }

        // PRODUCT SECTION
        if (!products.isEmpty()) {
            sb.append(String.format("%n🥤 **MUA LẺ**:%n%n"));

            for (Product p : products) {
                sb.append(String.format("• %s — %,.0f VNĐ%n",
                        p.getName(),
                        p.getPrice().doubleValue()));
            }
        }

        sb.append(System.lineSeparator())
                .append("💡 Bạn có thể thêm combo/đồ ăn khi đặt vé trên website/app.");

        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // TOOL 15 — Chương trình khuyến mãi
    // ────────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Tra cứu các chương trình khuyến mãi, mã giảm giá đang có hiệu lực.
            Dùng khi khách hỏi: 'Có khuyến mãi gì không?', 'Có mã giảm giá không?',
            'Chương trình ưu đãi?', 'Giảm giá vé xem phim?', 'Voucher?',
            'Có coupon nào không?', 'Mã khuyến mãi?', 'Đang có ưu đãi gì?'
            """)
    @Transactional(readOnly = true)
    public String getActivePromotions() {
        log.info("[CHATBOT TOOL] getActivePromotions called");
        LocalDate today = LocalDate.now();
        var promoPage = promotionRepository.findActivePromotions(today, PageRequest.of(0, 10));
        List<Promotion> promotions = promoPage.getContent();
        if (promotions.isEmpty()) {
            return "Hiện tại rạp chưa có chương trình khuyến mãi nào đang diễn ra. " +
                    "Bạn hãy theo dõi website để cập nhật ưu đãi mới nhất nhé!";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎉 **CHƯƠNG TRÌNH KHUYẾN MÃI ĐANG CÓ**:%n%n"));
        promotions.forEach(p -> {
            sb.append(String.format("• **%s**%n", p.getName()));
            sb.append(String.format("  🏷️ Mã: `%s`%n", p.getCode()));
            // Mô tả giảm giá
            if (p.getDiscountType() == DiscountType.PERCENTAGE) {
                sb.append(String.format("  💰 Giảm **%,.0f%%**", p.getDiscountValue().doubleValue()));
                if (p.getMaxDiscount() != null) {
                    sb.append(String.format(" (tối đa %,.0f VNĐ)",
                            p.getMaxDiscount().doubleValue()));
                }
                sb.append(String.format("%n"));
            } else {
                sb.append(String.format("  💰 Giảm **%,.0f VNĐ**%n",
                        p.getDiscountValue().doubleValue()));
            }
            // Đơn tối thiểu
            if (p.getMinOrderValue() != null && p.getMinOrderValue().doubleValue() > 0) {
                sb.append(String.format("  📌 Đơn tối thiểu: %,.0f VNĐ%n",
                        p.getMinOrderValue().doubleValue()));
            }
            // Thời hạn
            sb.append(String.format("  📅 Hiệu lực: %s → %s%n",
                    p.getStartDate().format(DATE_FMT), p.getEndDate().format(DATE_FMT)));
            // Số lượng còn lại
            int remaining = p.getQuantity() - p.getUsedQuantity();
            sb.append(String.format("  📊 Còn lại: %d/%d lượt%n", remaining, p.getQuantity()));
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append(String.format("  📝 %s%n", p.getDescription()));
            }
            sb.append(String.format("%n"));
        });
        sb.append("💡 Nhập mã khuyến mãi khi thanh toán trên website/app để được giảm giá!");
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper — parse ngày dd/MM/yyyy
    // ────────────────────────────────────────────────────────────────────────────
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            // Thử parse yyyy-MM-dd (ISO) nếu AI truyền dạng khác
            try {
                return LocalDate.parse(dateStr.trim());
            } catch (DateTimeParseException e2) {
                log.warn("[CHATBOT TOOL] Cannot parse date: {}", dateStr);
                return null;
            }
        }
    }

    private String formatCombo(Combo c) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("• **%s** — **%,.0f VNĐ**%n",
                c.getName(),
                c.getPrice().doubleValue()));

        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            sb.append(String.format("  📝 %s%n", c.getDescription()));
        }

        if (c.getItems() != null && !c.getItems().isEmpty()) {
            String items = c.getItems().stream()
                    .map(item -> String.format("%s x%d",
                            item.getProduct().getName(),
                            item.getQuantity()))
                    .collect(Collectors.joining(", "));

            sb.append("  Bao gồm: ").append(items).append(System.lineSeparator());
        }

        if (Boolean.TRUE.equals(c.isDiscounted())
                && c.getDiscountPercentage() != null) {

            sb.append(String.format("  🏷️ Tiết kiệm %,.0f%%%n",
                    c.getDiscountPercentage().doubleValue()));
        }

        return sb.toString();
    }
}
