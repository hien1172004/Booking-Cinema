package org.example.cinemaBooking.Service.Movie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Movie.AddPeopleToMovieRequest;
import org.example.cinemaBooking.DTO.Request.Movie.PeopleRoleRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMoviePeopleRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieCastResponse;
import org.example.cinemaBooking.DTO.Response.Movie.MoviePeopleResponse;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Entity.MoviePeople;
import org.example.cinemaBooking.Entity.People;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MoviePeopleMapper;
import org.example.cinemaBooking.Repository.MoviePeopleRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.example.cinemaBooking.Repository.PeopleRepository;
import org.example.cinemaBooking.Shared.enums.MovieRole;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class MoviePeopleService {

    PeopleRepository peopleRepository;
    MovieRepository movieRepository;
    MoviePeopleRepository moviePeopleRepository;
    MoviePeopleMapper moviePeopleMapper;

    // ==================== ADD PEOPLE TO MOVIE ====================

    /**
     * Thêm nhiều người vào phim (BULK)
     */
    @Transactional
    public List<MoviePeopleResponse> addPeopleToMovie(String movieId, AddPeopleToMovieRequest request) {
        log.info("===== ADD PEOPLE TO MOVIE =====");
        log.info("Movie ID: {}, Request size: {}", movieId, request.people().size());

        // 1. Kiểm tra movie tồn tại
        Movie movie = findMovieById(movieId);

        // 2. Lấy danh sách people đã có trong movie
        Set<String> existingPeopleIds = moviePeopleRepository.findPeopleIdsByMovieId(movieId);
        log.info("Existing people in movie: {}", existingPeopleIds.size());

        // 3. VALIDATION - Dùng chung với UPDATE
        Set<PeopleRoleRequest> uniqueRequests = new HashSet<>(request.people());
        validatePeopleRequest(movieId, uniqueRequests, false, existingPeopleIds);

        // 4. Lọc ra những people mới (chưa có trong movie)
        List<PeopleRoleRequest> newPeopleRequests = uniqueRequests.stream()
                .filter(pr -> !existingPeopleIds.contains(pr.peopleId()))
                .toList();

        log.info("Requested: {}, New to add: {}", request.people().size(), newPeopleRequests.size());

        // 5. Nếu không có người mới, trả về danh sách hiện tại
        if (newPeopleRequests.isEmpty()) {
            log.info("No new people to add for movie: {}", movieId);
            return getCurrentMoviePeople(movieId);
        }

        // 6. Lấy thông tin chi tiết của những người mới
        Map<String, People> peopleMap = getPeopleMap(newPeopleRequests);

        // 7. Tạo MoviePeople entities
        List<MoviePeople> moviePeopleToAdd = buildMoviePeopleList(movie, newPeopleRequests, peopleMap);

        // 8. Save all
        List<MoviePeople> saved = moviePeopleRepository.saveAll(moviePeopleToAdd);
        moviePeopleRepository.flush();

        log.info("Successfully added {} people to movie: {}", saved.size(), movieId);

        // 9. Trả về danh sách đầy đủ
        return getCurrentMoviePeople(movieId);
    }

//    /**
//     * Thêm 1 người vào phim (SINGLE)
//     */
//    @Transactional
//    public MoviePeopleResponse addOnePeopleToMovie(String movieId, String peopleId, String role) {
//        AddPeopleToMovieRequest request = new AddPeopleToMovieRequest(
//                List.of(new PeopleRoleRequest(peopleId, role))
//        );
//        return addPeopleToMovie(movieId, request).get(0);
//    }

    // ==================== UPDATE PEOPLE IN MOVIE ====================

    /**
     * Update role cho nhiều người trong phim (REPLACE - giống MovieImage)
     */
    @Transactional
    public List<MoviePeopleResponse> updateMoviePeople(String movieId, UpdateMoviePeopleRequest request) {
        log.info("===== UPDATE MOVIE PEOPLE (REPLACE) =====");
        log.info("Movie ID: {}", movieId);

        // 1. Kiểm tra movie tồn tại
        Movie movie = findMovieById(movieId);

        // 2. Lấy danh sách people từ request (loại bỏ duplicate)
        Set<PeopleRoleRequest> uniqueRequests = new HashSet<>(request.people());
        log.info("Requested people (unique): {}", uniqueRequests.size());

        // 3. Lấy danh sách hiện tại trong DB
        List<MoviePeople> existingMoviePeople = moviePeopleRepository.findByMovieId(movieId);
        Map<String, MoviePeople> existingMap = existingMoviePeople.stream()
                .collect(Collectors.toMap(
                        mp -> mp.getPeople().getId(),
                        Function.identity()
                ));

        Set<String> existingPeopleIds = existingMap.keySet();
        log.info("Existing people: {}", existingPeopleIds.size());

        // 4. VALIDATION - Dùng chung với ADD
        validatePeopleRequest(movieId, uniqueRequests, true, existingPeopleIds);

        // 5. Lấy danh sách people IDs từ request
        Set<String> requestedPeopleIds = uniqueRequests.stream()
                .map(PeopleRoleRequest::peopleId)
                .collect(Collectors.toSet());

        // ===== 6. DELETE: Những người có trong DB nhưng KHÔNG có trong request =====
        List<String> toDeleteIds = existingPeopleIds.stream()
                .filter(id -> !requestedPeopleIds.contains(id))
                .toList();

        if (!toDeleteIds.isEmpty()) {
            log.info("People to delete: {}", toDeleteIds);
            List<MoviePeople> toDelete = toDeleteIds.stream()
                    .map(existingMap::get)
                    .toList();
            moviePeopleRepository.deleteAllInBatch(toDelete);
            log.info("Deleted {} people from movie", toDelete.size());
        }

        // ===== 7. ADD: Những người có trong request nhưng KHÔNG có trong DB =====
        Set<String> newPeopleIds = requestedPeopleIds.stream()
                .filter(id -> !existingPeopleIds.contains(id))
                .collect(Collectors.toSet());

        List<MoviePeople> toAdd = new ArrayList<>();

        if (!newPeopleIds.isEmpty()) {
            log.info("New people to add: {}", newPeopleIds);

            // Lấy thông tin people mới
            Map<String, People> peopleMap = getPeopleMapByIds(newPeopleIds);

            // Tạo map role từ request
            Map<String, String> roleMap = uniqueRequests.stream()
                    .collect(Collectors.toMap(
                            PeopleRoleRequest::peopleId,
                            PeopleRoleRequest::role
                    ));

            // Tạo entities cho người mới
            for (String peopleId : newPeopleIds) {
                People people = peopleMap.get(peopleId);
                if (people == null) {
                    throw new AppException(ErrorCode.PEOPLE_NOT_FOUND);
                }

                MoviePeople moviePeople = MoviePeople.builder()
                        .movie(movie)
                        .people(people)
                        .movieRole(MovieRole.valueOf(roleMap.get(peopleId)))
                        .build();

                toAdd.add(moviePeople);
            }

            if (!toAdd.isEmpty()) {
                moviePeopleRepository.saveAll(toAdd);
                log.info("Added {} new people to movie", toAdd.size());
            }
        }

        // ===== 8. UPDATE: Những người có trong cả DB và request =====
        Set<String> updateIds = requestedPeopleIds.stream()
                .filter(existingPeopleIds::contains)
                .collect(Collectors.toSet());

        List<MoviePeople> toUpdate = new ArrayList<>();

        if (!updateIds.isEmpty()) {
            log.info("People to update (role may change): {}", updateIds);

            Map<String, String> roleMap = uniqueRequests.stream()
                    .collect(Collectors.toMap(
                            PeopleRoleRequest::peopleId,
                            PeopleRoleRequest::role
                    ));

            for (String peopleId : updateIds) {
                MoviePeople mp = existingMap.get(peopleId);
                MovieRole newRole = MovieRole.valueOf(roleMap.get(peopleId));

                if (mp.getMovieRole() != newRole) {
                    log.debug("Updating people {} role from {} to {}",
                            peopleId, mp.getMovieRole(), newRole);
                    mp.setMovieRole(newRole);
                    toUpdate.add(mp);
                }
            }

            if (!toUpdate.isEmpty()) {
                moviePeopleRepository.saveAll(toUpdate);
                log.info("Updated roles for {} people", toUpdate.size());
            }
        }

        moviePeopleRepository.flush();

        // 9. Log tổng kết
        log.info("UPDATE SUMMARY - Movie: {}", movieId);
        log.info("  - Requested: {}", requestedPeopleIds.size());
        log.info("  - Deleted: {}", toDeleteIds.size());
        log.info("  - Added: {}", newPeopleIds.size());
        log.info("  - Updated: {}", updateIds.size());

        // 10. Trả về danh sách mới nhất
        return getCurrentMoviePeople(movieId);
    }

//    /**
//     * Update role cho 1 người (SINGLE)
//     */
//    @Transactional
//    public MoviePeopleResponse updateOnePeopleRole(String movieId, String peopleId, String role) {
//        UpdateMoviePeopleRequest request = new UpdateMoviePeopleRequest(
//                List.of(new PeopleRoleRequest(peopleId, role))
//        );
//        return updateMoviePeople(movieId, request).get(0);
//    }

//    /**
//     * Swap role giữa 2 người
//     */
//    @Transactional
//    public List<MoviePeopleResponse> swapPeopleRoles(String movieId, String peopleId1, String peopleId2) {
//        log.info("Swapping roles between {} and {} in movie: {}", peopleId1, peopleId2, movieId);
//
//        MoviePeople mp1 = findMoviePeople(movieId, peopleId1);
//        MoviePeople mp2 = findMoviePeople(movieId, peopleId2);
//
//        MovieRole role1 = mp1.getMovieRole();
//        MovieRole role2 = mp2.getMovieRole();
//
//        List<PeopleRoleRequest> requests = List.of(
//                new PeopleRoleRequest(peopleId1, role2.name()),
//                new PeopleRoleRequest(peopleId2, role1.name())
//        );
//
//        return updateMoviePeople(movieId, new UpdateMoviePeopleRequest(requests));
//    }
//
//    /**
//     * Bulk update tất cả thành 1 role
//     */
//    @Transactional
//    public List<MoviePeopleResponse> bulkUpdateAllToRole(String movieId, String role, List<String> peopleIds) {
//        List<PeopleRoleRequest> requests = peopleIds.stream()
//                .map(id -> new PeopleRoleRequest(id, role))
//                .toList();
//        return updateMoviePeople(movieId, new UpdateMoviePeopleRequest(requests));
//    }

    // ==================== REMOVE PEOPLE FROM MOVIE ====================

    /**
     * Xóa 1 người khỏi phim
     */
    @Transactional
    public void removePeopleFromMovie(String movieId, String peopleId) {
        log.info("Removing people {} from movie: {}", peopleId, movieId);

        int deleted = moviePeopleRepository.deleteByMovieIdAndPeopleId(movieId, peopleId);

        if (deleted == 0) {
            throw new AppException(ErrorCode.MOVIE_PEOPLE_NOT_FOUND);
        }

        log.info("Successfully removed people {} from movie: {}", peopleId, movieId);
    }

    /**
     * Xóa nhiều người khỏi phim
     */
    @Transactional
    public void removeMultiplePeopleFromMovie(String movieId, List<String> peopleIds) {
        log.info("Removing {} people from movie: {}", peopleIds.size(), movieId);

        for (String peopleId : peopleIds) {
            moviePeopleRepository.deleteByMovieIdAndPeopleId(movieId, peopleId);
        }

        log.info("Successfully removed {} people from movie: {}", peopleIds.size(), movieId);
    }

    /**
     * Xóa tất cả người khỏi phim
     */
    @Transactional
    public void removeAllPeopleFromMovie(String movieId) {
        log.info("Removing all people from movie: {}", movieId);
        moviePeopleRepository.deleteByMovieId(movieId);
        log.info("Successfully removed all people from movie: {}", movieId);
    }

    // ==================== GET METHODS ====================

    /**
     * Lấy tất cả people trong movie
     */
    public List<MovieCastResponse> getPeopleByMovie(String movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new AppException(ErrorCode.MOVIE_NOT_FOUND);
        }

        List<MoviePeople> moviePeoples = moviePeopleRepository.findByMovieId(movieId);
        log.info("Found {} people in movie: {}", moviePeoples.size(), movieId);

        return moviePeoples.stream()
                .map(moviePeopleMapper::toMovieCastResponse)
                .toList();
    }





    // ==================== VALIDATION METHODS ====================

    /**
     * VALIDATION CHUNG cho cả ADD và UPDATE
     */
    private void validatePeopleRequest(String movieId,
                                       Set<PeopleRoleRequest> requests,
                                       boolean isUpdate,
                                       Set<String> existingPeopleIds) {

        log.info("Validating {} request for movie: {}", isUpdate ? "UPDATE" : "ADD", movieId);

        // RULE 1: Request không được null hoặc empty
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // RULE 2: Không được duplicate peopleId trong cùng request
        Set<String> uniqueIds = requests.stream()
                .map(PeopleRoleRequest::peopleId)
                .collect(Collectors.toSet());

        if (uniqueIds.size() != requests.size()) {
            throw new AppException(ErrorCode.DUPLICATE_PEOPLE_IN_REQUEST);
        }

        // RULE 3: Tất cả people phải tồn tại trong hệ thống
        validatePeopleExistInSystem(uniqueIds);

        // RULE 4: Role phải hợp lệ
        validateRoles(requests.stream().map(PeopleRoleRequest::role).toList());

        // RULE 5: QUAN TRỌNG - Chỉ được có 1 DIRECTOR
        validateDirectorRule(movieId, requests, isUpdate, existingPeopleIds);

        log.info("Validation passed for {} request", isUpdate ? "UPDATE" : "ADD");
    }

    /**
     * Validate rule DIRECTOR - xử lý khác nhau cho ADD và UPDATE
     */
    private void validateDirectorRule(String movieId,
                                      Set<PeopleRoleRequest> requests,
                                      boolean isUpdate,
                                      Set<String> existingPeopleIds) {

        // Đếm số DIRECTOR trong request
        long directorInRequest = requests.stream()
                .filter(r -> "DIRECTOR".equals(r.role()))
                .count();

        if (directorInRequest > 1) {
            throw new AppException(ErrorCode.MOVIE_CANNOT_HAVE_MULTIPLE_DIRECTORS);
        }

        // Nếu không có DIRECTOR trong request -> OK
        if (directorInRequest == 0) {
            return;
        }

        // CÓ DIRECTOR trong request - cần kiểm tra thêm
        String newDirectorId = requests.stream()
                .filter(r -> "DIRECTOR".equals(r.role()))
                .findFirst()
                .get()
                .peopleId();

        // Đếm số DIRECTOR hiện tại trong movie
        long existingDirector = moviePeopleRepository.countByMovieIdAndMovieRole(
                movieId, MovieRole.DIRECTOR);

        if (!isUpdate) {
            // ADD: total = existing + request
            long totalAfterAdd = existingDirector + directorInRequest;
            if (totalAfterAdd > 1) {
                throw new AppException(ErrorCode.MOVIE_CANNOT_HAVE_MULTIPLE_DIRECTORS);
            }
        } else {
            // UPDATE: kiểm tra xem có DIRECTOR khác không
            if (existingDirector > 0) {
                // Nếu người này chưa phải DIRECTOR, mà đã có DIRECTOR khác -> lỗi
                if (!existingPeopleIds.contains(newDirectorId) ||
                        (existingPeopleIds.contains(newDirectorId) &&
                                !isPeopleDirector(movieId, newDirectorId))) {
                    throw new AppException(ErrorCode.MOVIE_ALREADY_HAS_DIRECTOR);
                }
            }
        }
    }

    /**
     * Kiểm tra 1 người có phải DIRECTOR trong movie không
     */
    private boolean isPeopleDirector(String movieId, String peopleId) {
        return moviePeopleRepository.findByMovieIdAndPeopleId(movieId, peopleId)
                .map(mp -> mp.getMovieRole() == MovieRole.DIRECTOR)
                .orElse(false);
    }

    /**
     * Validate people tồn tại trong hệ thống
     */
    private void validatePeopleExistInSystem(Set<String> peopleIds) {
        long count = peopleRepository.countByIdIn(peopleIds);
        if (count != peopleIds.size()) {
            Set<String> foundIds = peopleRepository.findAllIdsIn(peopleIds);
            Set<String> notFoundIds = new HashSet<>(peopleIds);
            notFoundIds.removeAll(foundIds);
            log.error("People not found in system: {}", notFoundIds);
            throw new AppException(ErrorCode.PEOPLE_NOT_FOUND);
        }
    }

    /**
     * Validate roles hợp lệ
     */
    private void validateRoles(List<String> roles) {
        for (String role : roles) {
            try {
                MovieRole.valueOf(role);
            } catch (IllegalArgumentException e) {
                log.error("Invalid role: {}", role);
                throw new AppException(ErrorCode.INVALID_MOVIE_ROLE);
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Tìm movie theo ID
     */
    private Movie findMovieById(String movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));
    }

    /**
     * Tìm MoviePeople theo movieId và peopleId
     */
    private MoviePeople findMoviePeople(String movieId, String peopleId) {
        return moviePeopleRepository.findByMovieIdAndPeopleId(movieId, peopleId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_PEOPLE_NOT_FOUND));
    }

    /**
     * Lấy Map People theo danh sách request
     */
    private Map<String, People> getPeopleMap(List<PeopleRoleRequest> requests) {
        Set<String> peopleIds = requests.stream()
                .map(PeopleRoleRequest::peopleId)
                .collect(Collectors.toSet());
        return getPeopleMapByIds(peopleIds);
    }

    /**
     * Lấy Map People theo Set IDs
     */
    private Map<String, People> getPeopleMapByIds(Set<String> peopleIds) {
        return peopleRepository.findAllById(peopleIds).stream()
                .collect(Collectors.toMap(People::getId, Function.identity()));
    }

    /**
     * Build danh sách MoviePeople entities
     */
    private List<MoviePeople> buildMoviePeopleList(Movie movie,
                                                   List<PeopleRoleRequest> requests,
                                                   Map<String, People> peopleMap) {
        return requests.stream()
                .map(pr -> {
                    People people = peopleMap.get(pr.peopleId());
                    return MoviePeople.builder()
                            .movie(movie)
                            .people(people)
                            .movieRole(MovieRole.valueOf(pr.role()))
                            .build();
                })
                .toList();
    }

    /**
     * Lấy danh sách MoviePeople hiện tại của movie
     */
    private List<MoviePeopleResponse> getCurrentMoviePeople(String movieId) {
        return moviePeopleRepository.findByMovieId(movieId).stream()
                .map(moviePeopleMapper::toMoviePeopleResponse)
                .toList();
    }
}