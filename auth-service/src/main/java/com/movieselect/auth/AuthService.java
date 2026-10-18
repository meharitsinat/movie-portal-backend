package com.movieselect.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.movieselect.auth.AuthDtos.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AuthService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long tokenLifetimeMinutes;

    AuthService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.lifetime-minutes:60}") long tokenLifetimeMinutes) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.tokenLifetimeMinutes = tokenLifetimeMinutes;
    }

    @Transactional
    AuthResponse createGuest(String name, String email) {
        UserAccount guest = users.save(new UserAccount(name.trim(), email.trim(), null, "GUEST"));
        return issueToken(guest);
    }

    @Transactional(readOnly = true)
    AuthResponse loginAdmin(String email, String password) {
        UserAccount admin = users.findFirstByEmailIgnoreCaseAndRole(email.trim(), "ADMIN")
                .filter(user -> user.getPasswordHash() != null
                        && passwordEncoder.matches(password, user.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return issueToken(admin);
    }

    private AuthResponse issueToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenLifetimeMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("movie-select-auth")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("roles", List.of(user.getRole()))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", expiresAt, user.getId(),
                user.getName(), user.getEmail(), user.getRole());
    }
}
