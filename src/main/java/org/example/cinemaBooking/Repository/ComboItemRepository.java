package org.example.cinemaBooking.Repository;

import org.example.cinemaBooking.Entity.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComboItemRepository extends JpaRepository<ComboItem, String> {
}
