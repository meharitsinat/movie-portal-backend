package com.movieselect.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AdminAccountInitializer implements CommandLineRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    AdminAccountInitializer(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "Configure both ADMIN_EMAIL and ADMIN_PASSWORD (at least 12 characters)");
        }
        UserAccount admin = users.findFirstByEmailIgnoreCaseAndRole(adminEmail.trim(), "ADMIN")
                .orElseGet(() -> new UserAccount(
                        "Administrator", adminEmail.trim(), null, "ADMIN"));
        if (admin.getPasswordHash() == null
                || !passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        }
        users.save(admin);
    }
}
