package org.example.cinemaBooking.Service.Seat;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Seat.BulkSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.CreateSeatRequest;
import org.example.cinemaBooking.DTO.Request.Seat.UpdateSeatRequest;
import org.example.cinemaBooking.DTO.Response.Seat.SeatResponse;
import org.example.cinemaBooking.Entity.Room;
import org.example.cinemaBooking.Entity.Seat;
import org.example.cinemaBooking.Entity.SeatType;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.SeatMapper;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Repository.SeatRepository;
import org.example.cinemaBooking.Repository.SeatTypeRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class SeatService {
    SeatRepository seatRepository;
    RoomRepository roomRepository;
    SeatTypeRepository seatTypeRepository;
    SeatMapper seatMapper;

    /**
     * Tạo một ghế đơn trong phòng.
     *
     * @param request thông tin ghế cần tạo
     * @return SeatResponse ghế vừa tạo
     */
    @CacheEvict(value = "seatsByRoom", key = "#request.roomId")
    public SeatResponse createSeat(CreateSeatRequest request) {
        Room room = roomRepository.findById(request.roomId()).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        SeatType seatType = getSeatTypeOrThrow(request.seatTypeId());
        if (seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(room.getId(), request.seatRow(), request.seatNumber())) {
            throw new AppException(ErrorCode.SEAT_ALREADY_EXISTS);
        }
        Seat seat = Seat.builder().seatRow(request.seatRow()).seatNumber(request.seatNumber()).room(room).seatType(seatType).build();
        return seatMapper.toResponse(seatRepository.save(seat));
    }

    /**
     * Tạo hàng loạt ghế cho một phòng.
     *
     * @param roomId  id phòng
     * @param request danh sách nhóm ghế (row + number + type)
     * @return danh sách ghế đã tạo
     */
    @Transactional
    @CacheEvict(value = "seatsByRoom", key = "#roomId")
    public List<SeatResponse> createBulkSeats(String roomId, BulkSeatRequest request) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        List<Seat> seatsToCreate = new ArrayList<>();
        for (BulkSeatRequest.SeatGroup group : request.seatGroups()) {
            if (group.rows().isEmpty() || group.numbers().isEmpty()) {
                log.warn("Empty seat group skipped: {}", group);
                continue;
            }
            SeatType seatType = seatTypeRepository.findById(group.seatTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEAT_TYPE_NOT_FOUND));
            group.rows().forEach(row -> group.numbers().forEach(number -> {
                if (seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(roomId, row, number)) {
                    log.warn("Seat {}{} already exists in room {}", row, number, roomId);
                } else {
                    seatsToCreate.add(Seat.builder()
                                    .room(room)
                                    .seatRow(row)
                                    .seatNumber(number)
                                    .seatType(seatType)
                                    .active(true)
                                    .build());
                }
            }));
        }
        if (seatsToCreate.isEmpty()) {
            log.warn("No seats created for room {}", roomId);
            return List.of();
        }
        List<Seat> saved = seatRepository.saveAll(seatsToCreate);
        log.info("Created {} seats for room {}", saved.size(), roomId);
        return saved.stream().map(seatMapper::toResponse).toList();
    }

    /**
     * Cập nhật thông tin ghế.
     *
     * @param seatId  id ghế
     * @param request dữ liệu cập nhật
     * @return SeatResponse sau khi update
     */
    @CacheEvict(value = "seatsByRoom", allEntries = true)
    public SeatResponse updateSeat(String seatId, UpdateSeatRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));
        if (request.seatTypeId() != null) {
            SeatType seatType = getSeatTypeOrThrow(request.seatTypeId());
            seat.setSeatType(seatType);
        }
        seatMapper.updateSeat(seat, request);
        return seatMapper.toResponse(seatRepository.save(seat));
    }

    /**
     * Xóa mềm ghế (soft delete).
     *
     * @param seatId id ghế
     */
    @CacheEvict(value = "seatsByRoom", allEntries = true)
    public void deleteSeat(String seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));
        seat.setDeletedAt(LocalDateTime.now());
        seatRepository.save(seat);
    }

    /**
     * Lấy thông tin ghế theo id.
     *
     * @param seatId id ghế
     * @return SeatResponse
     */
    public SeatResponse getSeat(String seatId) {
        return seatRepository.findById(seatId)
                .map(seatMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));
    }

    /**
     * Lấy danh sách ghế theo phòng (CACHE).
     * <p>
     * Cache key = roomId
     *
     * @param roomId id phòng
     * @return danh sách ghế
     */
    @Cacheable(value = "seatsByRoom", key = "#roomId")
    public List<SeatResponse> getSeatsByRoom(String roomId) {
        return seatRepository.findAllByRoomIdFetch(roomId)
                .stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    /**
     * Helper: lấy SeatType hoặc throw exception.
     */
    private SeatType getSeatTypeOrThrow(String seatTypeId) {
        return seatTypeRepository.findById(seatTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_TYPE_NOT_FOUND));
    }
}