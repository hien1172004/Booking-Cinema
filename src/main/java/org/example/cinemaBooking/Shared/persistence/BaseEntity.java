package org.example.cinemaBooking.Shared.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    /**
     * Optimistic locking cho concurrent update
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }
}
