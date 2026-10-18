package com.movieselect.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(
            @NotEmpty @Size(max = 50) List<@NotNull UUID> movieIds) {}

    public record SetPriceRequest(
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price) {}

    public record OrderResponse(
            UUID id,
            UUID guestId,
            String guestName,
            String guestEmail,
            BigDecimal price,
            String status,
            List<UUID> movieIds,
            String stripeCheckoutSessionId,
            Instant createdAt,
            Instant updatedAt) {
        static OrderResponse from(GuestOrder order) {
            return new OrderResponse(order.getId(), order.getGuestId(), order.getGuestName(),
                    order.getGuestEmail(), order.getPrice(), order.getStatus(),
                    order.getMovies().stream().map(OrderMovie::getMovieId).toList(),
                    order.getStripeCheckoutSessionId(), order.getCreatedAt(), order.getUpdatedAt());
        }
    }

    public record CheckoutResponse(String sessionId, String checkoutUrl) {}
}
