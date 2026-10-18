package com.movieselect.auth;

import com.movieselect.auth.AuthDtos.AdminLoginRequest;
import com.movieselect.auth.AuthDtos.AuthResponse;
import com.movieselect.auth.AuthDtos.GuestRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse createGuest(@Valid @RequestBody GuestRequest request) {
        return authService.createGuest(request.name(), request.email());
    }

    @PostMapping("/admin/login")
    AuthResponse adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return authService.loginAdmin(request.email(), request.password());
    }
}
