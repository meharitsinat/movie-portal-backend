package com.movieselect.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findFirstByEmailIgnoreCaseAndRole(String email, String role);
}
