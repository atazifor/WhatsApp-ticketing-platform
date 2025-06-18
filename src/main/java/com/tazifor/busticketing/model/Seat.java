package com.tazifor.busticketing.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.util.*;
import java.time.*;

@Entity
@Table(name = "seat",
    uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "seat_number"}))
@Data
public class Seat {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(name = "is_sold", nullable = false)
    private Boolean isSold = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "travel_class_id", nullable = false)
    private TravelClass travelClass;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsSold() {
        if (this.isSold) {
            throw new IllegalStateException("Seat already sold");
        }
        this.isSold = true;
        this.updatedAt = LocalDateTime.now();
    }
}


