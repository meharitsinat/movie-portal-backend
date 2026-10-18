package com.movieselect.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface GuestOrderRepository extends JpaRepository<GuestOrder, UUID> {
    List<GuestOrder> findAllByGuestIdOrderByCreatedAtDesc(UUID guestId);
    List<GuestOrder> findAllByOrderByCreatedAtDesc();
    Optional<GuestOrder> findByIdAndGuestId(UUID id, UUID guestId);
}
