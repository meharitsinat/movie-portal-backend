package com.movieselect.order;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.movieselect.order.OrderDtos.CreateOrderRequest;
import com.movieselect.order.OrderDtos.OrderResponse;
import com.stripe.model.checkout.Session;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderService {
    private final GuestOrderRepository orders;
    private final CatalogClient catalogClient;

    OrderService(GuestOrderRepository orders, CatalogClient catalogClient) {
        this.orders = orders;
        this.catalogClient = catalogClient;
    }

    void validateMovies(String accessToken, CreateOrderRequest request) {
        if (request.movieIds().size() != new HashSet<>(request.movieIds()).size()) {
            throw new IllegalArgumentException("Duplicate movie IDs are not allowed");
        }
        request.movieIds().forEach(id -> catalogClient.requireActiveMovie(id, accessToken));
    }

    @Transactional
    OrderResponse create(UUID guestId, String guestName, String guestEmail,
                         CreateOrderRequest request) {
        GuestOrder order = new GuestOrder(guestId, guestName, guestEmail);
        request.movieIds().forEach(order::addMovie);
        return OrderResponse.from(orders.save(order));
    }

    @Transactional(readOnly = true)
    List<OrderResponse> list(UUID userId, boolean admin) {
        List<GuestOrder> results = admin
                ? orders.findAllByOrderByCreatedAtDesc()
                : orders.findAllByGuestIdOrderByCreatedAtDesc(userId);
        return results.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    OrderResponse get(UUID id, UUID userId, boolean admin) {
        GuestOrder order = findAccessibleOrder(id, userId, admin);
        return OrderResponse.from(order);
    }

    @Transactional
    OrderResponse setPrice(UUID id, BigDecimal price) {
        GuestOrder order = orders.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        order.setCheckoutPrice(price);
        return OrderResponse.from(order);
    }

    @Transactional
    void recordCheckoutSession(UUID id, UUID guestId, boolean admin, String sessionId) {
        GuestOrder order = findAccessibleOrder(id, guestId, admin);
        order.setStripeCheckoutSessionId(sessionId);
    }

    @Transactional
    void markPaid(Session session) {
        String orderId = session.getMetadata() == null ? null : session.getMetadata().get("orderId");
        if (orderId == null || session.getAmountTotal() == null || session.getCurrency() == null) {
            throw new IllegalArgumentException("Stripe checkout session is missing order details");
        }
        UUID id;
        try {
            id = UUID.fromString(orderId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Stripe checkout session has an invalid order ID", exception);
        }

        GuestOrder order = orders.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        long expectedAmount = order.getPrice().movePointRight(2).longValueExact();
        if (!"usd".equalsIgnoreCase(session.getCurrency())
                || session.getAmountTotal() != expectedAmount
                || !id.toString().equals(session.getClientReferenceId())) {
            throw new IllegalArgumentException("Stripe checkout details do not match the order");
        }
        order.markPaid(session.getId());
    }

    private GuestOrder findAccessibleOrder(UUID id, UUID userId, boolean admin) {
        if (admin) {
            return orders.findById(id).orElseThrow(() -> new NoSuchElementException("Order not found"));
        }
        return orders.findByIdAndGuestId(id, userId)
                .orElseThrow(() -> new AccessDeniedException("You cannot access this order"));
    }
}
