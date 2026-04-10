package org.example.cinemaBooking.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class People extends SoftDeletableEntity {

    @Column(nullable = false)
    String name;

    @Column(length = 100)
    String nation;

    @Column(nullable = false, length = 500)
    String avatarUrl;

    LocalDate dob;
}
