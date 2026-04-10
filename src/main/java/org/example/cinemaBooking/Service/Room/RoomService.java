package org.example.cinemaBooking.Service.Room;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Room.CreateRoomRequest;
import org.example.cinemaBooking.DTO.Request.Room.UpdateRoomRequest;
import org.example.cinemaBooking.DTO.Response.Room.RoomResponse;

import org.example.cinemaBooking.Entity.Cinema;
import org.example.cinemaBooking.Entity.Room;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.RoomMapper;
import org.example.cinemaBooking.Repository.CinemaRepository;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {
    RoomMapper roomMapper;
    RoomRepository roomRepository;
    CinemaRepository cinemaRepository;

    public RoomResponse createRoom(CreateRoomRequest request) {
        Room room = roomMapper.toRoomEntity(request);
        room.setCinema(cinemaRepository.findCinemaById(request.cinemaId()));
        room.setStatus(Status.ACTIVE);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    public void deleteRoomByID(String roomId) {
        Optional<Room> exitingRoom = roomRepository.findById(roomId);
        if (exitingRoom.isPresent()) {
            if (exitingRoom.get().getStatus() == Status.INACTIVE) {
                throw new AppException(ErrorCode.ROOM_ALREADY_INACTIVE);
            }
            roomRepository.delete(exitingRoom.get());
        } else {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }


    public RoomResponse getRoomByID(String roomId) {
        Optional<Room> exitingRoom = roomRepository.findById(roomId);
        if (exitingRoom.isPresent()) {
            return roomMapper.toResponse(exitingRoom.get());
        } else {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    public void toggleRoomStatus(String roomId) {
        Optional<Room> exitingRoom = roomRepository.findById(roomId);
        if (exitingRoom.isPresent()) {
            if (exitingRoom.get().getStatus() == Status.ACTIVE) {
                exitingRoom.get().setStatus(Status.INACTIVE);
            } else {
                exitingRoom.get().setStatus(Status.ACTIVE);
            }
        } else {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    public PageResponse<RoomResponse> getAllRooms(int page, int size, String sortBy, String direction, String keyword) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, size, sort);
        Page<Room> roomPage;
        if (keyword != null && !keyword.isBlank()) {
            roomPage = roomRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
        } else {
            roomPage = roomRepository.findAllByDeletedFalse(pageable);
        }
        return PageResponse.<RoomResponse>builder().page(page).size(size).totalElements(roomPage.getTotalElements()).totalPages(roomPage.getTotalPages()).items(roomPage.getContent().stream().map(roomMapper::toResponse).toList()).build();
    }
    public PageResponse<RoomResponse> getRoomByCinema(String cinemaId,int page, int size, String sortBy, String direction) {
        Cinema cinema = cinemaRepository.findCinemaById(cinemaId);
        int pageNumber = page > 0 ? page - 1 : 0;
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, size, sort);
        Page<Room> roomPage;
        roomPage = roomRepository.findRoomsByCinema(cinema, pageable);
        return PageResponse.<RoomResponse>builder().page(page).size(size).totalElements(roomPage.getTotalElements()).totalPages(roomPage.getTotalPages()).items(roomPage.getContent().stream().map(roomMapper::toResponse).toList()).build();
    }

    public RoomResponse updateRoom(String id, UpdateRoomRequest request) {
        Optional<Room> exitingRoom = roomRepository.findById(id);
        if (exitingRoom.isPresent()) {
            Room room = exitingRoom.get();
            roomMapper.updateRoom(request, room);
            return roomMapper.toResponse(roomRepository.save(room));
        } else {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }
}