package com.movieselect.order;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_movies")
public class OrderMovie {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private GuestOrder order;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    protected OrderMovie() {
    }

    OrderMovie(GuestOrder order, UUID movieId) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.movieId = movieId;
    }

    public UUID getMovieId() { return movieId; }
}
