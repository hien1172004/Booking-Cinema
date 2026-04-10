package org.example.cinemaBooking.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category extends SoftDeletableEntity {

    @Column(unique = true, nullable = false)
    String name;

    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    private Set<Movie> movies = new HashSet<>();
}
