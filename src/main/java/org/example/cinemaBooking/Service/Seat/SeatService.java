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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public SeatResponse createSeat(CreateSeatRequest request) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        SeatType seatType = getSeatTypeOrThrow(request.seatTypeId());

        if (seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(
                room.getId(), request.seatRow(), request.seatNumber()
        )) {
            throw new AppException(ErrorCode.SEAT_ALREADY_EXISTS);
        }

        Seat seat = Seat.builder()
                .seatRow(request.seatRow())
                .seatNumber(request.seatNumber())
                .room(room)
                .seatType(seatType)
                .build();

        return seatMapper.toResponse(seatRepository.save(seat));
    }

    @Transactional
    public List<SeatResponse> createBulkSeats(String roomId, BulkSeatRequest request) {
        // Kiểm tra phòng tồn tại
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        List<Seat> seatsToCreate = new ArrayList<>();

        // Duyệt từng nhóm ghế
        for (BulkSeatRequest.SeatGroup group : request.seatGroups()) {
            // Validate input
            if (group.rows().isEmpty() || group.numbers().isEmpty()) {
                log.warn("Seat group has empty rows or numbers, skipping group: {}", group);
                continue;
            }

            // Kiểm tra seatType tồn tại
            SeatType seatType = seatTypeRepository.findById(group.seatTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.SEAT_TYPE_NOT_FOUND));

            // Tạo ghế cho từng row và number
            group.rows().forEach(row ->
                    group.numbers().forEach(number -> {
                        if (seatRepository.existsByRoomIdAndSeatRowAndSeatNumberAndDeletedAtIsNull(
                                roomId, row, number)) {
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
                    })
            );
        }

        if (seatsToCreate.isEmpty()) {
            log.warn("No new seats to create for room {}", roomId);
            return List.of();
        }

        // Lưu tất cả ghế
        List<Seat> savedSeats = seatRepository.saveAll(seatsToCreate);
        log.info("Created {} seats for room {}", seatsToCreate.size(), roomId);

        return savedSeats.stream()
                .map(seatMapper::toResponse)
                .toList();
    }


    public SeatResponse updateSeat(String seatId, UpdateSeatRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (request.seatTypeId() != null) {
            SeatType seatType = getSeatTypeOrThrow(request.seatTypeId());
            seat.setSeatType(seatType);
        }

        seatMapper.updateSeat(seat, request);

        return seatMapper.toResponse(seatRepository.save(seat));
    }

    public void deleteSeat(String seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));

        seat.setDeletedAt(java.time.LocalDateTime.now());
        seatRepository.save(seat);
    }

    public SeatResponse getSeat(String seatId) {
        return seatRepository.findById(seatId)
                .map(seatMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_NOT_FOUND));
    }

    public List<SeatResponse> getSeatsByRoom(String roomId) {
        return seatRepository.findAllByRoomIdFetch(roomId)
                .stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    //internal
    private SeatType getSeatTypeOrThrow(String seatTypeId) {
        return seatTypeRepository.findById(seatTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_TYPE_NOT_FOUND));
    }


}
