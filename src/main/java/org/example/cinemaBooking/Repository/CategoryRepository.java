package org.example.cinemaBooking.Repository;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import org.example.cinemaBooking.Entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
//    boolean existsByNameAndDeletedFalse(String name);
//
//    Optional<Category> findByName(String name);
//
//    Page<Category> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword, Pageable pageable);
//
//    Page<Category> findByDeletedFalse(Pageable pageable);

    boolean existsByName(@NotBlank(message = "CATEGORY_NAME_REQUIRED") String name);

    Page<Category> findByNameContainingIgnoreCase(String key, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM movie_category WHERE category_id = :id", nativeQuery = true)
    void deleteCategoryRelations(@Param("id") String id);
}
