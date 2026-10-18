package com.movieselect.order;

import java.math.BigDecimal;
import java.util.UUID;

import com.movieselect.order.OrderDtos.CheckoutResponse;
import com.movieselect.order.OrderDtos.OrderResponse;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class CheckoutService {
    private final OrderService orderService;
    private final String stripeSecretKey;
    private final String successUrl;
    private final String cancelUrl;

    CheckoutService(
            OrderService orderService,
            @Value("${stripe.secret-key:}") String stripeSecretKey,
            @Value("${stripe.success-url}") String successUrl,
            @Value("${stripe.cancel-url}") String cancelUrl) {
        this.orderService = orderService;
        this.stripeSecretKey = stripeSecretKey;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    CheckoutResponse createCheckout(UUID orderId, UUID guestId) {
        OrderResponse order = orderService.get(orderId, guestId, false);
        if (!"CHECKOUT_READY".equals(order.status())) {
            throw new IllegalArgumentException("Order is not ready for checkout");
        }
        long amount = toMinorUnits(order.price());
        if (amount < 50) {
            throw new IllegalArgumentException("Stripe Checkout requires an order price of at least $0.50");
        }

        StripeClient stripe = stripeClient();
        try {
            if (order.stripeCheckoutSessionId() != null) {
                Session existing = stripe.checkout().sessions()
                        .retrieve(order.stripeCheckoutSessionId());
                if ("open".equals(existing.getStatus()) && existing.getUrl() != null) {
                    return new CheckoutResponse(existing.getId(), existing.getUrl());
                }
                if ("complete".equals(existing.getStatus())) {
                    throw new IllegalArgumentException("Checkout for this order has already completed");
                }
            }

            Session session = stripe.checkout().sessions().create(
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl(successUrl)
                            .setCancelUrl(cancelUrl)
                            .setClientReferenceId(order.id().toString())
                            .putMetadata("orderId", order.id().toString())
                            .addLineItem(SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(amount)
                                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                                    .builder()
                                                    .setName("Movie selection order " + order.id())
                                                    .build())
                                            .build())
                                    .build())
                            .build());
            orderService.recordCheckoutSession(orderId, guestId, false, session.getId());
            return new CheckoutResponse(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Unable to create Stripe checkout session", exception);
        }
    }

    private StripeClient stripeClient() {
        if (stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe checkout is not configured");
        }
        return new StripeClient(stripeSecretKey);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
