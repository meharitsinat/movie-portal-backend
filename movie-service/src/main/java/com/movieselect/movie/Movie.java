package com.movieselect.movie;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "movies")
public class Movie {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    private Integer year;
    @Column(length = 100)
    private String genre;
    @Column(length = 2048)
    private String poster;
    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Movie() {
    }

    Movie(String title, Integer year, String genre, String poster, String description) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.poster = poster;
        this.description = description;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    void update(String title, Integer year, String genre, String poster, String description) {
        if (title != null) this.title = title;
        if (year != null) this.year = year;
        if (genre != null) this.genre = genre;
        if (poster != null) this.poster = poster;
        if (description != null) this.description = description;
    }

    void deactivate() {
        active = false;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public Integer getYear() { return year; }
    public String getGenre() { return genre; }
    public String getPoster() { return poster; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
