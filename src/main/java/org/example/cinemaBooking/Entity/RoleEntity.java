package org.example.cinemaBooking.Entity;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;

import java.util.Set;

@Entity(name = "Role")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@RequiredArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class RoleEntity extends SoftDeletableEntity {
    String name;
    String description;

    @ManyToMany(mappedBy = "roles")
    Set<UserEntity> users;


}
