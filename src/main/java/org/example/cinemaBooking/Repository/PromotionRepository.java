package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, String> {
    Optional<Promotion> findByCode(String code);

    @Query("""
SELECT p FROM Promotion p
WHERE (:code IS NULL OR p.code LIKE %:code%)
AND (:name IS NULL OR p.name LIKE %:name%)
AND (:startDate IS NULL OR p.startDate >= :startDate)
AND (:endDate IS NULL OR p.endDate <= :endDate)
AND (:minOrderValue IS NULL OR p.minOrderValue >= :minOrderValue)
AND (:maxOrderValue IS NULL OR p.minOrderValue <= :maxOrderValue)
AND p.deleted = false
""")
    Page<Promotion> findWithFilters(String code, String name, LocalDate startDate, LocalDate endDate, BigDecimal minOrderValue, BigDecimal maxOrderValue, Pageable pageable);

    @Query("""
SELECT p FROM Promotion p
WHERE p.deleted = false
AND p.startDate <= :now
AND p.endDate >= :now
AND p.quantity > p.usedQuantity
""")
    Page<Promotion> findActivePromotions(LocalDate now, Pageable pageable);

    @Modifying
    @Query("""
    UPDATE Promotion p
    SET p.usedQuantity = p.usedQuantity + 1
    WHERE p.id = :id AND p.usedQuantity < p.quantity
""")
    int increaseUsedQuantityIfAvailable(String id);

    @Modifying
    @Query("""
    UPDATE Promotion p
    SET p.usedQuantity = p.usedQuantity - 1
    WHERE p.id = :id AND p.usedQuantity > 0
""")
    int decreaseUsedQuantityIfUsed(String id);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.code       = :code
          AND p.startDate  <= :today
          AND p.endDate    >= :today
          AND p.deletedAt  IS NULL
        """)
    Optional<Promotion> findActiveByCode(
            @Param("code")  String code,
            @Param("today") LocalDate today
    );

}
