package com.movieselect.movie;

import com.movieselect.security.ApiExceptionHandler;
import com.movieselect.security.JwtSecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({JwtSecurityConfiguration.class, ApiExceptionHandler.class})
public class MovieApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieApplication.class, args);
    }
}
