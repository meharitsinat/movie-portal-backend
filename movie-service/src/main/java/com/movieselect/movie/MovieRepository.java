package com.movieselect.movie;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findAllByActiveTrueOrderByTitleAsc();
    List<Movie> findAllByOrderByTitleAsc();
    Optional<Movie> findByIdAndActiveTrue(UUID id);
}
