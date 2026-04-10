package org.example.cinemaBooking.Service.Cinema;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Cinema.CreateCinemaRequest;
import org.example.cinemaBooking.DTO.Request.Cinema.UpdateCinemaRequest;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaMovieResponse;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaResponse;
import org.example.cinemaBooking.DTO.Response.Room.RoomBasicResponse;
import org.example.cinemaBooking.Entity.Cinema;
import org.example.cinemaBooking.Entity.Room;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.CinemaMapper;
import org.example.cinemaBooking.Mapper.RoomMapper;
import org.example.cinemaBooking.Repository.CinemaRepository;
import org.example.cinemaBooking.Repository.RoomRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.Shared.enums.Status;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CinemaService {

    CinemaRepository cinemaRepository;
    CinemaMapper cinemaMapper;
    RoomRepository roomRepository;
    RoomMapper roomMapper;

    /**
     * Tạo mới một Rạp chiếu phim.
     * <p>Sẽ xoá toàn bộ cache "cinemas" để cập nhật lại danh sách rạp.</p>
     *
     * @param request Dữ liệu tạo rạp chiếu
     * @return CinemaResponse chứa thông tin rạp vừa tạo
     */
    @CacheEvict(value = "cinemas", allEntries = true)
    public CinemaResponse createCinema(CreateCinemaRequest request){
        Cinema cinema = cinemaMapper.toCinema(request);
        cinema.setStatus(Status.ACTIVE);
        return cinemaMapper.toResponse(cinemaRepository.save(cinema));
    }

    /**
     * Xoá mềm (Soft Delete) một rạp chiếu phim dựa trên ID.
     * <p>Xoá tất cả cache liên quan đến rạp (danh sách chung, chi tiết rạp và danh sách bộ phim thuộc rạp).</p>
     *
     * @param id ID của rạp chiếu phim cần xoá
     * @throws AppException Nếu không tìm thấy rạp chiếu hoặc rạp đã bị khoá
     */
    @Caching(evict = {
            @CacheEvict(value = "cinemas", allEntries = true),
            @CacheEvict(value = "cinema", key = "#id"),
            @CacheEvict(value = "cinema-movies", allEntries = true)
    })
    public void deleteCinemaById(String id){
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));

        if(cinema.getStatus() == Status.INACTIVE){
            throw new AppException(ErrorCode.CINEMA_ALREADY_INACTIVE);
        }

        cinema.setDeleted(true);
        cinemaRepository.save(cinema);
    }

    /**
     * Lấy thông tin chi tiết của một rạp chiếu phim.
     * <p>Kết quả được lưu vào cache "cinema" với key là ID của rạp.</p>
     *
     * @param id ID của rạp chiếu phim
     * @return CinemaResponse chứa thông tin chi tiết của rạp
     */
    @Cacheable(value = "cinema", key = "#id")
    public CinemaResponse getCinemaById(String id){
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));

        return cinemaMapper.toResponse(cinema);
    }

    /**
     * Thay đổi trạng thái của rạp chiếu phim (ACTIVE <-> INACTIVE).
     * <p>Sau khi đổi trạng thái, xoà toàn bộ các cache liên quan của rạp.</p>
     *
     * @param id ID của rạp chiếu phim cần đổi trạng thái
     */
    @Caching(evict = {
            @CacheEvict(value = "cinemas", allEntries = true),
            @CacheEvict(value = "cinema", key = "#id"),
            @CacheEvict(value = "cinema-movies", allEntries = true)
    })
    public void toggleCinemaStatus(String id){
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));

        if(cinema.getStatus() == Status.ACTIVE){
            cinema.setStatus(Status.INACTIVE);
        } else {
            cinema.setStatus(Status.ACTIVE);
        }

        cinemaRepository.save(cinema);
    }

    /**
     * Lấy danh sách rạp chiếu phim (hỗ trợ phân trang, sắp xếp và tìm kiếm).
     * Dữ liệu được cache lại theo kích thước trang, chiều sắp xếp và từ khoá.
     *
     * @param page      Số thứ tự trang (bắt đầu từ 1)
     * @param size      Số lượng bản ghi trên một trang
     * @param sortBy    Trường cần sắp xếp
     * @param direction Hướng sắp xếp (asc hoặc desc)
     * @param keyword   Từ khoá tìm kiếm theo tên (nếu null sẽ chuyển thành chuỗi rỗng trong cache key)
     * @return PageResponse chứa danh sách rạp chiếu phim đã phân trang
     */
    @Cacheable(value = "cinemas"
            ,key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction + '-' + (#keyword ?: '')")
    public PageResponse<CinemaResponse> getAllCinemas(
            int page,
            int size,
            String sortBy,
            String direction,
            String keyword
    ){
        int pageNumber = page > 0 ? page - 1 : 0;

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        Page<Cinema> cinemaPage;

        if(keyword != null && !keyword.isBlank()){
            cinemaPage = cinemaRepository
                    .findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
        } else {
            cinemaPage = cinemaRepository.findAllByDeletedFalse(pageable);
        }

        return PageResponse.<CinemaResponse>builder()
                .page(page)
                .size(size)
                .totalElements(cinemaPage.getTotalElements())
                .totalPages(cinemaPage.getTotalPages())
                .items(cinemaPage.getContent().stream()
                        .map(cinemaMapper::toResponse)
                        .toList())
                .build();
    }

    /**
     * Cập nhật thông tin rạp chiếu phim.
     * <p>Xoá bộ nhớ đệm liệt kê rạp, rạp chi tiết và danh sách phim đang chứa rạp chiếu này.</p>
     *
     * @param id      ID của rạp chiếu phim cần cập nhật
     * @param request Dữ liệu cập nhật mới
     * @return CinemaResponse chứa thông tin rạp sau khi cập nhật
     */
    @Caching(evict = {
            @CacheEvict(value = "cinemas", allEntries = true),
            @CacheEvict(value = "cinema", key = "#id"),
            @CacheEvict(value = "cinema-movies", allEntries = true)
    })
    public CinemaResponse updateCinema(String id, UpdateCinemaRequest request){
        Cinema cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CINEMA_NOT_FOUND));

        cinemaMapper.updateCinema(request, cinema);

        return cinemaMapper.toResponse(cinemaRepository.save(cinema));
    }

    /**
     * Lấy danh sách các bộ phim đang được chiếu tại rạp vào một ngày cụ thể (có phân trang).
     * Dữ liệu này được cache cẩn thận vì danh mục lịch chiếu cho ngày cụ thể ít khi thay đổi.
     *
     * @param cinemaId  ID rạp chiếu phim
     * @param date      Ngày cần xem lịch chiếu
     * @param page      Số thứ tự trang
     * @param size      Số lượng bản ghi mỗi trang
     * @param sortBy    Tiêu chí sắp xếp
     * @param direction Hướng sắp xếp
     * @return PageResponse danh sách bộ phim đang chiếu
     */
    @Cacheable(
            value = "cinema-movies",
            key = "#cinemaId + '-' + #date + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #direction"
    )
    @Transactional(readOnly = true)
    public PageResponse<CinemaMovieResponse> getMoviesByCinemaAndDate(
            String cinemaId,
            LocalDate date,
            int page,
            int size,
            String sortBy,
            String direction) {

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        int pageNumber = page > 0 ? page - 1 : 0;
        // Tạo Pageable với sorting
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        // Lấy Page từ repository
        Page<CinemaMovieResponse> moviePage = cinemaRepository.findMoviesByCinemaAndDate(
                cinemaId, from, to, pageable);

        // Convert Page sang PageResponse
        return PageResponse.<CinemaMovieResponse>builder()
                .items(moviePage.getContent())
                .page(moviePage.getNumber())
                .size(moviePage.getSize())
                .totalElements(moviePage.getTotalElements())
                .totalPages(moviePage.getTotalPages())
                .build();
    }

    /**
     * Lấy danh sách các phòng xem phim của một rạp (có phân trang).
     * Dữ liệu các phòng hiếm khi thay đổi nên rất thích hợp để lưu bộ nhớ đệm dài hạn.
     *
     * @param cinemaId  ID của rạp chiếu phim
     * @param page      Số thứ tự trang
     * @param size      Kích thước trang
     * @param sortBy    Tiêu chí sắp xếp
     * @param direction Hướng sắp xếp
     * @return PageResponse thông tin cơ bản về phòng chiếu của rạp
     */
    @Cacheable(value = "cinema-rooms",
               key = "#cinemaId + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #direction")
    public PageResponse<RoomBasicResponse> getRoomsByCinema(
            String cinemaId,
            int page,
            int size,
            String sortBy,
            String direction) {
        int pageNumber = page > 0 ? page - 1 : 0;

        Sort.Direction sort = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(sort, sortBy));

        Page<Room> roomPage = roomRepository.findByCinemaId(cinemaId, pageable);

        List<RoomBasicResponse> roomResponses = roomPage.getContent()
                .stream().map(roomMapper::toBasicResponse)
                .toList();

        return PageResponse.<RoomBasicResponse>builder()
                .page(page)
                .size(size)
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .items(roomResponses)
                .build();
    }


}