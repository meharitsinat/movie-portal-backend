package com.movieselect.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "guest_orders")
public class GuestOrder {
    @Id
    private UUID id;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "guest_name", nullable = false, length = 120)
    private String guestName;

    @Column(name = "guest_email", nullable = false, length = 320)
    private String guestEmail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "stripe_checkout_session_id", unique = true)
    private String stripeCheckoutSessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("movieId ASC")
    private List<OrderMovie> movies = new ArrayList<>();

    protected GuestOrder() {
    }

    GuestOrder(UUID guestId, String guestName, String guestEmail) {
        this.id = UUID.randomUUID();
        this.guestId = guestId;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.price = BigDecimal.ZERO.setScale(2);
        this.status = "PENDING";
    }

    void addMovie(UUID movieId) {
        movies.add(new OrderMovie(this, movieId));
    }

    void setCheckoutPrice(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Price must be non-negative and have at most two decimal places");
        }
        if (stripeCheckoutSessionId != null && amount.compareTo(price) != 0) {
            throw new IllegalArgumentException("Expire the current checkout session before changing the price");
        }
        if (!"PENDING".equals(status) && !"CHECKOUT_READY".equals(status)) {
            throw new IllegalArgumentException("Only pending orders can be priced");
        }
        price = amount.setScale(2);
        status = "CHECKOUT_READY";
    }

    void setStripeCheckoutSessionId(String sessionId) {
        if (!"CHECKOUT_READY".equals(status)) {
            throw new IllegalArgumentException("Order is not ready for checkout");
        }
        stripeCheckoutSessionId = sessionId;
    }

    boolean markPaid(String sessionId) {
        if ("PAID".equals(status) && sessionId.equals(stripeCheckoutSessionId)) {
            return false;
        }
        if (!"CHECKOUT_READY".equals(status) || !sessionId.equals(stripeCheckoutSessionId)) {
            return false;
        }
        status = "PAID";
        return true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getGuestId() { return guestId; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    List<OrderMovie> getMovies() { return movies; }
}
