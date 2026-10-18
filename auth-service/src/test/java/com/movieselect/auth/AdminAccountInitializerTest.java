package com.movieselect.auth;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAccountInitializerTest {
    @Test
    void synchronizesConfiguredPasswordForExistingAdmin() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String previousPassword = "previous-password";
        String configuredPassword = "new-configured-password";
        UserAccount admin = new UserAccount(
                "Administrator", "admin@example.com",
                passwordEncoder.encode(previousPassword), "ADMIN");
        when(users.findFirstByEmailIgnoreCaseAndRole("admin@example.com", "ADMIN"))
                .thenReturn(Optional.of(admin));

        new AdminAccountInitializer(
                users, passwordEncoder, "admin@example.com", configuredPassword).run();

        assertFalse(passwordEncoder.matches(previousPassword, admin.getPasswordHash()));
        assertTrue(passwordEncoder.matches(configuredPassword, admin.getPasswordHash()));
        verify(users).save(admin);
    }
}
