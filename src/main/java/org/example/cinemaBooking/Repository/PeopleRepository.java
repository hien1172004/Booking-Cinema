package org.example.cinemaBooking.Repository;


import org.example.cinemaBooking.Entity.People;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface PeopleRepository extends JpaRepository<People, String> {
    Page<People> findByNameContainingIgnoreCase(String key, Pageable pageable);

    long countByIdIn(Set<String> peopleIds);

    @Query("SELECT p.id FROM People p WHERE p.id IN :ids")
    Set<String> findAllIdsIn(@Param("ids")Set<String> peopleIds);
}
