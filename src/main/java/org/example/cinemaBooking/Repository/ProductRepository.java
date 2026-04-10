package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByName(String name);

    Page<Product> findByNameContainingIgnoreCaseAndDeletedFalse(String keyword, Pageable pageable);

    Page<Product> findAllByDeletedFalse(Pageable pageable);


    Page<Product> findByActiveTrueAndDeletedFalse(Pageable pageable);
}
