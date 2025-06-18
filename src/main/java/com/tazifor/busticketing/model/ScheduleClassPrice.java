package com.tazifor.busticketing.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "schedule_class_price",
    uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_id", "travel_class_id"}))
@Data
public class ScheduleClassPrice {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(optional = false)
    @JoinColumn(name = "travel_class_id", nullable = false)
    private TravelClass travelClass;

    @Column(nullable = false)
    private Integer price;

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
}


