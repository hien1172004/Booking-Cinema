package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.UsedPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsedPromotionRepository extends JpaRepository<UsedPromotion, String> {

    @Query("SELECT SUM(u.usageCount) FROM UsedPromotion u WHERE u.user.id = :userId AND u.promotion.id = :promotionId")
    Integer getTotalUsageCountByUserAndPromotion(String userId, String promotionId);

    Optional<UsedPromotion> findByUserIdAndPromotionId(String userId, String promotionId);
}
