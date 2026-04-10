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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    /**
     * Tạo mới một phòng chiếu.
     * <p>Xoá bộ đệm các danh sách phòng chiếu và phòng chiếu theo rạp để hiển thị dữ liệu mới.</p>
     *
     * @param request Thông tin phòng chiếu mới
     * @return RoomResponse Chi tiết phòng chiếu vừa tạo
     */
    @Caching(evict = {
            @CacheEvict(value = "rooms", allEntries = true),
            @CacheEvict(value = "rooms-by-cinema", allEntries = true),
            @CacheEvict(value = "cinema-rooms", allEntries = true)
    })
    public RoomResponse createRoom(CreateRoomRequest request) {
        Room room = roomMapper.toRoomEntity(request);
        room.setCinema(cinemaRepository.findCinemaById(request.cinemaId()));
        room.setStatus(Status.ACTIVE);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    /**
     * Xóa một phòng chiếu.
     * <p>Xóa bộ đệm liên quan đến phòng chiếu bị xóa.</p>
     *
     * @param roomId ID của phòng chiếu
     */
    @Caching(evict = {
            @CacheEvict(value = "rooms", allEntries = true),
            @CacheEvict(value = "rooms-by-cinema", allEntries = true),
            @CacheEvict(value = "cinema-rooms", allEntries = true),
            @CacheEvict(value = "room", key = "#roomId")
    })
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


    /**
     * Lấy thông tin phòng chiếu theo ID.
     * <p>Kết quả được lưu vào cache "room".</p>
     *
     * @param roomId ID của phòng chiếu
     * @return RoomResponse
     */
    @Cacheable(value = "room", key = "#roomId")
    public RoomResponse getRoomByID(String roomId) {
        Optional<Room> exitingRoom = roomRepository.findById(roomId);
        if (exitingRoom.isPresent()) {
            return roomMapper.toResponse(exitingRoom.get());
        } else {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    /**
     * Chuyển đổi trạng thái phòng chiếu (ví dụ: đang bảo trì, hoạt động...).
     * <p>Xóa bộ đệm để đảm bảo trạng thái hiển thị đúng.</p>
     *
     * @param roomId ID phòng chiếu
     */
    @Caching(evict = {
            @CacheEvict(value = "rooms", allEntries = true),
            @CacheEvict(value = "rooms-by-cinema", allEntries = true),
            @CacheEvict(value = "cinema-rooms", allEntries = true),
            @CacheEvict(value = "room", key = "#roomId")
    })
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

    /**
     * Lấy danh sách tất cả phòng chiếu trong hệ thống.
     * <p>Kết quả được lưu cache "rooms" kèm theo thông số phân trang và tìm kiếm.</p>
     *
     * @param page Số trang
     * @param size Kích thước hiển thị
     * @param sortBy Cột sắp xếp
     * @param direction Hướng sắp xếp
     * @param keyword Từ khóa tìm kiếm
     * @return PageResponse
     */
    @Cacheable(value = "rooms", key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction + '-' + (#keyword ?: '')")
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

    /**
     * Lấy danh sách phòng theo cụm Rạp.
     * <p>Lưu ở cache "rooms-by-cinema" để tối ưu tốc độ người dùng xem danh sách phòng khả dụng theo Rạp.</p>
     *
     * @param cinemaId ID của rạp
     * @param page Trang hiển thị
     * @param size Kích thước trang
     * @param sortBy Cột sắp xếp
     * @param direction Hướng sắp xếp
     * @return PageResponse
     */
    @Cacheable(value = "rooms-by-cinema", key = "#cinemaId + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #direction")
    public PageResponse<RoomResponse> getRoomByCinema(String cinemaId,int page, int size, String sortBy, String direction) {
        Cinema cinema = cinemaRepository.findCinemaById(cinemaId);
        int pageNumber = page > 0 ? page - 1 : 0;
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, size, sort);
        Page<Room> roomPage;
        roomPage = roomRepository.findRoomsByCinema(cinema, pageable);
        return PageResponse.<RoomResponse>builder().page(page).size(size).totalElements(roomPage.getTotalElements()).totalPages(roomPage.getTotalPages()).items(roomPage.getContent().stream().map(roomMapper::toResponse).toList()).build();
    }

    /**
     * Cập nhật thông tin phòng chiếu (tên, số lượng ghế,...).
     *
     * @param id ID của phòng chiếu
     * @param request Yêu cầu thay đổi
     * @return RoomResponse
     */
    @Caching(evict = {
            @CacheEvict(value = "rooms", allEntries = true),
            @CacheEvict(value = "rooms-by-cinema", allEntries = true),
            @CacheEvict(value = "cinema-rooms", allEntries = true),
            @CacheEvict(value = "room", key = "#id")
    })
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