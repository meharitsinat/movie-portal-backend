package com.movieselect.movie;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MovieDtos {
    private MovieDtos() {}

    public record MovieRequest(
            @NotBlank @Size(max = 200) String title,
            @Min(1888) @Max(2200) Integer year,
            @Size(max = 100) String genre,
            @Size(max = 2048) String poster,
            @Size(max = 10000) String description) {}

    public record MovieUpdateRequest(
            @jakarta.validation.constraints.NotBlank @Size(max = 200) String title,
            @Min(1888) @Max(2200) Integer year,
            @Size(max = 100) String genre,
            @Size(max = 2048) String poster,
            @Size(max = 10000) String description,
            Boolean active) {}

    public record MovieResponse(
            UUID id,
            String title,
            Integer year,
            String genre,
            String poster,
            String description,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
        static MovieResponse from(Movie movie) {
            return new MovieResponse(movie.getId(), movie.getTitle(), movie.getYear(),
                    movie.getGenre(), movie.getPoster(), movie.getDescription(),
                    movie.isActive(), movie.getCreatedAt(), movie.getUpdatedAt());
        }
    }
}
