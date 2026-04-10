package org.example.cinemaBooking.Service.Seat;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Response.Seat.SeatTypeResponse;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.SeatTypeMapper;
import org.example.cinemaBooking.Repository.SeatTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class SeatTypeService {
    SeatTypeRepository seatTypeRepository;
    SeatTypeMapper seatTypeMapper;


    public List<SeatTypeResponse> getAll() {
        return seatTypeRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(seatTypeMapper::toResponse)
                .toList();
    }

    public SeatTypeResponse getById(String id) {
        return seatTypeRepository.findById(id)
                .map(seatTypeMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SEAT_TYPE_NOT_FOUND));
    }

}
