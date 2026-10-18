package com.movieselect.movie;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.movieselect.movie.MovieDtos.MovieRequest;
import com.movieselect.movie.MovieDtos.MovieResponse;
import com.movieselect.movie.MovieDtos.MovieUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
class MovieController {
    private final MovieService movieService;

    MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies")
    List<MovieResponse> activeMovies() {
        return movieService.getActiveMovies();
    }

    @GetMapping("/movies/{id}")
    MovieResponse activeMovie(@PathVariable UUID id) {
        return movieService.getActiveMovie(id);
    }

    @GetMapping("/admin/movies")
    @PreAuthorize("hasRole('ADMIN')")
    List<MovieResponse> allMovies() {
        return movieService.getAllMovies();
    }

    @PostMapping("/admin/movies")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        MovieResponse movie = movieService.create(request);
        return ResponseEntity.created(URI.create("/api/movies/" + movie.id())).body(movie);
    }

    @PostMapping("/admin/movies/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<List<MovieResponse>> createAll(
            @RequestBody @NotEmpty @Size(max = 100) List<@Valid MovieRequest> requests) {
        List<MovieResponse> createdMovies = movieService.createAll(requests);
        return ResponseEntity.created(URI.create("/api/admin/movies")).body(createdMovies);
    }

    @PatchMapping("/admin/movies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    MovieResponse update(@PathVariable UUID id, @Valid @RequestBody MovieUpdateRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/admin/movies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        movieService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
