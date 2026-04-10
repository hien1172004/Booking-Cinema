package org.example.cinemaBooking.Service.Movie;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Movie.CreatePeopleRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdatePeopleRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MoviePeopleResponse;
import org.example.cinemaBooking.DTO.Response.Movie.PeopleResponse;

import org.example.cinemaBooking.Entity.MoviePeople;
import org.example.cinemaBooking.Entity.People;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MoviePeopleMapper;
import org.example.cinemaBooking.Mapper.PeopleMapper;
import org.example.cinemaBooking.Repository.MoviePeopleRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.PeopleRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PeopleService {
    PeopleRepository peopleRepository;
    MoviePeopleRepository moviePeopleRepository;
    MovieRepository movieRepository;
    PeopleMapper peopleMapper;
    MoviePeopleMapper moviePeopleMapper;

    /**
     * Tạo mới một diễn viên/đạo diễn/nhân sự (People).
     * <p>Xoá cache "peoples" để cập nhật lại danh sách tìm kiếm.</p>
     *
     * @param request Dữ liệu tạo mới People
     * @return PeopleResponse thông tin người vừa tạo
     */
    @CacheEvict(value = "peoples", allEntries = true)
    public PeopleResponse createPeople(CreatePeopleRequest request) {
        var people = peopleMapper.toEntity(request);
        var savedPeople = peopleRepository.save(people);
        log.info("[PEOPLE_SERVICE] - Create people with id: {}", savedPeople.getId());
        return peopleMapper.toResponse(savedPeople);
    }

    /**
     * Cập nhật thông tin diễn viên/đạo diễn.
     * <p>Xoá bộ đệm của danh sách "peoples" và thông tin chi tiết "people".</p>
     *
     * @param id      ID của người cần cập nhật
     * @param request Thông tin mới
     * @return PeopleResponse thông tin sau khi cập nhật
     */
    @Caching(evict = {
            @CacheEvict(value = "peoples", allEntries = true),
            @CacheEvict(value = "people", key = "#id")
    })
    public PeopleResponse updatePeople(String id, UpdatePeopleRequest request) {
        var people = peopleRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PEOPLE_NOT_FOUND));
        peopleMapper.updatePeople(request, people);
        var updatedPeople = peopleRepository.save(people);
        log.info("[PEOPLE_SERVICE] - Update people with id: {}", updatedPeople.getId());
        return peopleMapper.toResponse(updatedPeople);
    }

    /**
     * Xoá diễn viên/đạo diễn khỏi hệ thống.
     * <p>Đồng thời xoá trong bảng liên kết với các phim và xoá các cache liên quan.</p>
     *
     * @param id ID của nhân sự cần xoá
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "peoples", allEntries = true),
            @CacheEvict(value = "people", key = "#id"),
            @CacheEvict(value = "movies-by-people", key = "#id")
    })
    public void deletePeople(String id){
        var people = peopleRepository.findById(id).orElseThrow(()
                -> new AppException(ErrorCode.PEOPLE_NOT_FOUND));
        moviePeopleRepository.deleteByPeopleId(id);
        // Xóa People
        peopleRepository.delete(people);
        log.info("[PEOPLE_SERVICE] - Delete people with id: {}", id);
    }

    /**
     * Lấy thông tin chi tiết của một nhân sự (diễn viên/đạo diễn).
     * <p>Kết quả được lưu vào cache "people" với key là ID của người đó.</p>
     *
     * @param id ID của nhân sự
     * @return PeopleResponse thông tin chi tiết
     */
    @Cacheable(value = "people", key = "#id")
    public PeopleResponse getPeopleById(String id) {
        var people = peopleRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PEOPLE_NOT_FOUND));
        log.info("[PEOPLE_SERVICE] - Get people with id: {}", id);
        return peopleMapper.toResponse(people);
    }

    /**
     * Lấy danh sách diễn viên/đạo diễn (có phân trang và tìm kiếm).
     * <p>Dữ liệu được lưu cache lại theo các tham số truy vấn.</p>
     *
     * @param page Số thứ tự trang
     * @param size Kích thước trang
     * @param key  Từ khóa tìm kiếm theo tên (nếu null sẽ chuyển thành chuỗi rỗng)
     * @return PageResponse danh sách nhân sự
     */
    @Cacheable(value = "peoples", key = "#page + '-' + #size + '-' + (#key ?: '')")
    public PageResponse<PeopleResponse> getAllPeoples(int page, int size, String key) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("name").ascending());
        Page<People> peoplePage =
                (key == null || key.isBlank())
                        ? peopleRepository.findAll(pageable)
                        : peopleRepository.findByNameContainingIgnoreCase(key, pageable);

        List<PeopleResponse> peopleResponses = peoplePage.getContent().stream()
                .map(peopleMapper::toResponse)
                .toList();
        log.info("[PEOPLE_SERVICE] - Get all peoples with page: {}, size: {}, key: {}", page, size, key);
        return PageResponse.<PeopleResponse>builder()
                .page(page)
                .size(size)
                .totalElements(peoplePage.getTotalElements())
                .totalPages(peoplePage.getTotalPages())
                .items(peopleResponses)
                .build();

    }

    /**
     * Lấy tất cả các phim mà một nhân sự (diễn viên/đạo diễn) tham gia.
     * <p>Dữ liệu được lưu cache "movies-by-people" với key là peopleId.</p>
     *
     * @param peopleId ID của nhân sự
     * @return Danh sách các vai trò và bộ phim tham gia
     */
    @Cacheable(value = "movies-by-people", key = "#peopleId")
    public List<MoviePeopleResponse> getMoviesByPeople(String peopleId) {
        if (!peopleRepository.existsById(peopleId)) {
            throw new AppException(ErrorCode.PEOPLE_NOT_FOUND);
        }

        List<MoviePeople> moviePeoples = moviePeopleRepository.findByPeopleId(peopleId);
        log.info("Found {} movies for people: {}", moviePeoples.size(), peopleId);

        return moviePeoples.stream()
                .map(moviePeopleMapper::toMoviePeopleResponse)
                .toList();
    }
}
