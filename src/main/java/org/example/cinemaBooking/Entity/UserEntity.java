package org.example.cinemaBooking.Entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.cinemaBooking.Shared.persistence.SoftDeletableEntity;
import org.example.cinemaBooking.Shared.enums.Gender;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class UserEntity extends SoftDeletableEntity {
    @Column(nullable = false, unique = true)
    String username;

    String password;

    String fullName;

    @Column(unique = true)
    String email;

    @Column(length = 20)
    String phone;

    @Builder.Default
    String avatarUrl = "https://cdn.kona-blue.com/upload/kona-blue_com/post/images/2024/09/18/457/avatar-mac-dinh-11.jpg";

    LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Builder.Default
    @Column(nullable = false)
    private boolean status = true;

    @ManyToMany
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    Set<RoleEntity> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<Notification> notifications;

}
