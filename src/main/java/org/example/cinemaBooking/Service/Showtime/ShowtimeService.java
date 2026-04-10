package org.example.cinemaBooking.Service.Showtime;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Showtime.CreateShowtimeRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.ShowtimeFilterRequest;
import org.example.cinemaBooking.DTO.Request.Showtime.UpdateShowtimeRequest;
import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeDetailResponse;

import org.example.cinemaBooking.DTO.Response.Showtime.ShowtimeSummaryResponse;
import org.example.cinemaBooking.Entity.*;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ShowtimeMapper;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Repository.ShowtimeRepository;
import org.example.cinemaBooking.Repository.spefication.ShowtimeSpecification;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.SeatStatus;
import org.example.cinemaBooking.Shared.enums.ShowTimeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ShowtimeService {
    ShowtimeRepository showtimeRepository;
    ShowtimeMapper showtimeMapper;
    MovieRepository movieRepository;
    RoomRepository roomRepository;
    private static final int BUFFER_MINUTES = 20;

    @Transactional
    public ShowtimeDetailResponse createShowtime(CreateShowtimeRequest request){
        Room room = getRoomById(request.roomId());
        Movie movie = getMovieById(request.movieId());

        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDuration() + BUFFER_MINUTES); // buffer 20 phút giữa các suất chiếu

       boolean conflict = showtimeRepository.existsConflict(
               room.getId(),
                "0", // excludeId = "0" để không loại trừ bất kỳ suất chiếu nào (vì đang tạo mới)
                request.startTime(), endTime
       );

       if(conflict) {
           throw new AppException(ErrorCode.SHOWTIME_CONFLICT);
       }


        Showtime showtime = showtimeMapper.toEntity(request);
        showtime.setRoom(room);
        showtime.setMovie(movie);

        List<ShowtimeSeat> seats = room.getSeats().stream()
                        .filter(Seat::isActive)
                                .map(seat ->ShowtimeSeat.builder()
                                        .seat(seat)
                                        .showtime(showtime)
                                        .status(SeatStatus.AVAILABLE)
                                        .build())
                                        .toList();

        showtime.getShowtimeSeats().addAll(seats);
        showtime.setAvailableSeats(seats.size());

        Showtime savedShowtime = showtimeRepository.save(showtime);
        log.info("Showtime created: id={}, movie={}, room={}, start={}",
                savedShowtime.getId(), movie.getTitle(), room.getName(), request.startTime());

        return showtimeMapper.toDetailResponse(savedShowtime);
    }

    @Transactional(readOnly = true)
    public ShowtimeDetailResponse getShowtimeById(String showtimeId){
        Showtime showtime = getShowtimeEntityById(showtimeId);
        return showtimeMapper.toDetailResponse(showtime);
    }

    @Transactional(readOnly = true)
    public PageResponse<ShowtimeSummaryResponse> getShowtime(ShowtimeFilterRequest request){
        int pageNumber = 0;
        if(request.page() > 0) {
            pageNumber = request.page() - 1; // client gửi page=1 thì backend sẽ query page=0
        }
        Pageable pageable = PageRequest.of(pageNumber, request.size());

        Page<Showtime> showtimePage = showtimeRepository.findAll(ShowtimeSpecification.of(request), pageable);

        List<ShowtimeSummaryResponse> showtimeList = showtimePage.getContent()
                .stream().map(showtimeMapper::toSummaryResponse)
                .toList();

        return PageResponse.<ShowtimeSummaryResponse>builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(showtimePage.getTotalElements())
                .totalPages(showtimePage.getTotalPages())
                .items(showtimeList)
                .build();

    }
    @Transactional(readOnly = true)
    public List<ShowtimeSummaryResponse> getShowtimeByMovieAndDate(String movieId, LocalDate date){
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = from.plusDays(1);

        List<Showtime> showtimes = showtimeRepository.findByMovieAndDateRange(
                movieId, from, to, ShowTimeStatus.CANCELLED);

        return showtimeMapper.toSummaryResponseList(showtimes);
    }


    @Transactional(readOnly = true)
    public List<ShowtimeSummaryResponse> getShowtimesByCinemaAndDate(String cinemaId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = from.plusDays(1);
        List<Showtime> showtimes = showtimeRepository.findByCinemaAndDateRange(
                cinemaId, from, to, ShowTimeStatus.CANCELLED);
        return showtimeMapper.toSummaryResponseList(showtimes);
    }


    @Transactional
    public ShowtimeDetailResponse updateShowtime(String showtimeId, UpdateShowtimeRequest request){
        Showtime showtime = getShowtimeEntityById(showtimeId);

        if(showtime.getStatus() != ShowTimeStatus.SCHEDULED) {
            throw new AppException(ErrorCode.SHOWTIME_STATE_INVALID);
        }

        if(request.roomId() != null && !request.roomId().equals(showtime.getRoom().getId())) {
            Room newRoom = getRoomById(request.roomId());
            showtime.setRoom(newRoom);
            showtime.getShowtimeSeats().clear();
            List<ShowtimeSeat> newSeats = newRoom.getSeats().stream()
                    .filter(Seat::isActive)
                    .map(seat -> ShowtimeSeat.builder()
                            .showtime(showtime)
                            .seat(seat)
                            .status(SeatStatus.AVAILABLE)
                            .build())
                    .toList();
            showtime.getShowtimeSeats().addAll(newSeats);
            showtime.setAvailableSeats(newSeats.size());
        }
        LocalDateTime newStartTime = request.startTime() != null ? request.startTime() : showtime.getStartTime();

        if(request.startTime() != null) {
            LocalDateTime newEndTime = newStartTime.plusMinutes(showtime.getMovie().getDuration() + BUFFER_MINUTES);
            boolean conflict = showtimeRepository.existsConflict(
                    showtime.getRoom().getId(),
                    showtimeId,
                    newStartTime, newEndTime
            );
            if (conflict) {
                throw new AppException(ErrorCode.SHOWTIME_CONFLICT);
            }
        }

        showtimeMapper.updateEntityFromRequest(request, showtime);
        Showtime updatedShowtime = showtimeRepository.save(showtime);
        log.info("Showtime updated: id={}, movie={}, room={}, start={}",
                updatedShowtime.getId(), updatedShowtime.getMovie().getTitle(),
                updatedShowtime.getRoom().getName(), updatedShowtime.getStartTime());
        return showtimeMapper.toDetailResponse(updatedShowtime);
    }


    @Transactional
    public ShowtimeDetailResponse cancelShowtime(String showtimeId){
        Showtime showtime = getShowtimeEntityById(showtimeId);

       if(showtime.getStatus() == ShowTimeStatus.FINISHED
               || showtime.getStatus() == ShowTimeStatus.CANCELLED) {
           throw new AppException(ErrorCode.SHOWTIME_STATE_INVALID);
       }

        showtime.setStatus(ShowTimeStatus.CANCELLED);
        // Giải phóng tất cả ghế đang bị LOCKED / BOOKED về AVAILABLE
        // (để refund flow xử lý riêng — ở đây chỉ cập nhật trạng thái ghế)
        showtime.getShowtimeSeats().forEach(ss -> {
            if (ss.getStatus() != SeatStatus.AVAILABLE) {
                ss.setStatus(SeatStatus.AVAILABLE);
                ss.setLockedUntil(null);
                ss.setLockedByUser(null);
            }
        });
        Showtime cancelledShowtime = showtimeRepository.save(showtime);
        log.warn("Showtime cancelled: id={}, movie={}, room={}, start={}",
                cancelledShowtime.getId(), cancelledShowtime.getMovie().getTitle(),
                cancelledShowtime.getRoom().getName(), cancelledShowtime.getStartTime());
        return showtimeMapper.toDetailResponse(cancelledShowtime);
    }

    @Transactional
    public void deleteShowtime(String showtimeId){
        Showtime showtime = getShowtimeEntityById(showtimeId);

       showtime.softDelete();
        showtimeRepository.save(showtime);
        log.warn("Showtime soft-deleted: id={}", showtimeId);
    }

    @Scheduled(cron = "0 * * * * *")   // mỗi phút đầu
    @Transactional
    public void startDueShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        List<Showtime> due = showtimeRepository.findScheduledShowtimesToStart(now);
        if (due.isEmpty()) return;

        due.forEach(s -> s.setStatus(ShowTimeStatus.ONGOING));
        showtimeRepository.saveAll(due);
        log.info("Started {} showtime(s) at {}", due.size(), now);
    }

    @Scheduled(cron = "30 * * * * *")  // mỗi phút lúc :30s
    @Transactional
    public void finishEndedShowtimes() {
        LocalDateTime now = LocalDateTime.now();
        List<Showtime> ongoing = showtimeRepository.findAllOngoing();

        List<Showtime> finished = ongoing.stream()
                .filter(Showtime::isFinished)
                .toList();

        // Cập nhật trạng thái rõ ràng sau khi lọc
        finished.forEach(s -> s.setStatus(ShowTimeStatus.FINISHED));

        if (finished.isEmpty()) return;

        showtimeRepository.saveAll(finished);
        log.info("Finished {} showtime(s) at {}", finished.size(), now);
    }
    private Showtime getShowtimeEntityById(String showtimeId){
        return showtimeRepository.findByIdWithDetails(showtimeId).orElseThrow(()
                -> new AppException(ErrorCode.SHOWTIME_NOT_FOUND));
    }
    private Room getRoomById(String roomId){
        return roomRepository.findByIdWithSeats(roomId).orElseThrow(()
                -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Movie getMovieById(String movieId){
        return movieRepository.findById(movieId).orElseThrow(()
                -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
    }
}
