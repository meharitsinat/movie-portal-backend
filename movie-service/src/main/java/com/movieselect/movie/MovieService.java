package com.movieselect.movie;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.movieselect.movie.MovieDtos.MovieRequest;
import com.movieselect.movie.MovieDtos.MovieResponse;
import com.movieselect.movie.MovieDtos.MovieUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MovieService {
    private final MovieRepository movies;

    MovieService(MovieRepository movies) {
        this.movies = movies;
    }

    @Transactional(readOnly = true)
    List<MovieResponse> getActiveMovies() {
        return movies.findAllByActiveTrueOrderByTitleAsc().stream().map(MovieResponse::from).toList();
    }

    @Transactional(readOnly = true)
    List<MovieResponse> getAllMovies() {
        return movies.findAllByOrderByTitleAsc().stream().map(MovieResponse::from).toList();
    }

    @Transactional(readOnly = true)
    MovieResponse getActiveMovie(UUID id) {
        return movies.findByIdAndActiveTrue(id)
                .map(MovieResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Movie not found"));
    }

    @Transactional
    MovieResponse create(MovieRequest request) {
        Movie movie = movies.save(new Movie(request.title().trim(), request.year(),
                request.genre(), request.poster(), request.description()));
        return MovieResponse.from(movie);
    }

    @Transactional
    List<MovieResponse> createAll(List<MovieRequest> requests) {
        List<Movie> savedMovies = movies.saveAll(requests.stream()
                .map(request -> new Movie(request.title().trim(), request.year(),
                        request.genre(), request.poster(), request.description()))
                .toList());
        return savedMovies.stream().map(MovieResponse::from).toList();
    }

    @Transactional
    MovieResponse update(UUID id, MovieUpdateRequest request) {
        Movie movie = findMovie(id);
        movie.update(request.title() == null ? null : request.title().trim(),
                request.year(), request.genre(), request.poster(), request.description());
        if (request.active() != null) {
            movie.setActive(request.active());
        }
        return MovieResponse.from(movie);
    }

    @Transactional
    void deactivate(UUID id) {
        findMovie(id).deactivate();
    }

    private Movie findMovie(UUID id) {
        return movies.findById(id).orElseThrow(() -> new NoSuchElementException("Movie not found"));
    }
}
