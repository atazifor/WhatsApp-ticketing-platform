package com.nourri.busticketing.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.util.*;
import java.time.*;

@Entity
@Table(name = "agency")
@Data
public class Agency {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean hasApi = false;

    private String apiBaseUrl;

    @Column(unique = true)
    private String slug;

    private String logoUrl;

    @Column(nullable = false)
    private Integer maxTicketsPerBooking = 4;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (hasApi == null) hasApi = false;
        if (maxTicketsPerBooking == null) maxTicketsPerBooking = 4;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


