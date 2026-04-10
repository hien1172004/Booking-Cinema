package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
        """)
    Page<Notification> findAllByUserId(
            @Param("userId") String userId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.user.id = :userId
          AND n.read = false
          AND n.deletedAt IS NULL
        """)
    int countUnreadByUserId(@Param("userId") String userId);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true
        WHERE n.user.id = :userId
          AND n.deletedAt IS NULL
        """)
    void markAllAsReadByUserId(@Param("userId") String userId);
}
