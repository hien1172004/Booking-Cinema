package org.example.cinemaBooking.Repository;


import org.example.cinemaBooking.Entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findUserEntityByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
SELECT DISTINCT u FROM UserEntity u
JOIN u.roles r
WHERE r.name = 'CUSTOMER'
AND (:key IS NULL OR
       LOWER(u.username) LIKE LOWER(CONCAT('%', :key, '%')) OR
       LOWER(u.email) LIKE LOWER(CONCAT('%', :key, '%')))
""")
    Page<UserEntity> searchUsers(String key, Pageable pageable);


    @Query("""
SELECT DISTINCT u FROM UserEntity u
JOIN u.roles r
WHERE r.name = 'STAFF'
AND (:key IS NULL OR
       LOWER(u.username) LIKE LOWER(CONCAT('%', :key, '%')) OR
       LOWER(u.email) LIKE LOWER(CONCAT('%', :key, '%')))
""")
    Page<UserEntity> searchStaffs(String key, Pageable pageable);

    @Query("""
        SELECT COUNT(u)
        FROM UserEntity u
        WHERE u.createdAt >= :start
          AND u.createdAt < :end
          AND u.deletedAt IS NULL
    """)
    int countUsers(@Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);
}
