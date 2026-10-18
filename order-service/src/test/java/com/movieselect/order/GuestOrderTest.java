package com.movieselect.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class GuestOrderTest {
    @Test
    void guestOrderStartsPendingWithNoCharge() {
        GuestOrder order = new GuestOrder(UUID.randomUUID(), "Guest", "guest@example.com");

        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("0.00"), order.getPrice());
    }

    @Test
    void adminPriceTransitionsOrderToCheckoutReady() {
        GuestOrder order = new GuestOrder(UUID.randomUUID(), "Guest", "guest@example.com");

        order.setCheckoutPrice(new BigDecimal("25.00"));

        assertEquals("CHECKOUT_READY", order.getStatus());
        assertEquals(new BigDecimal("25.00"), order.getPrice());
    }

    @Test
    void orderRejectsInvalidPrices() {
        GuestOrder order = new GuestOrder(UUID.randomUUID(), "Guest", "guest@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> order.setCheckoutPrice(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class,
                () -> order.setCheckoutPrice(new BigDecimal("1.001")));
    }

    @Test
    void onlyTheAssociatedStripeSessionCanMarkAnOrderPaidAndWebhookIsIdempotent() {
        GuestOrder order = new GuestOrder(UUID.randomUUID(), "Guest", "guest@example.com");
        order.setCheckoutPrice(new BigDecimal("25.00"));
        order.setStripeCheckoutSessionId("cs_test_123");

        assertFalse(order.markPaid("cs_test_other"));
        assertTrue(order.markPaid("cs_test_123"));
        assertFalse(order.markPaid("cs_test_123"));
        assertEquals("PAID", order.getStatus());
    }
}
