package com.movieselect.order;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class StripeWebhookController {
    private final OrderService orderService;
    private final String webhookSecret;

    StripeWebhookController(
            OrderService orderService,
            @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.orderService = orderService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/api/webhooks/stripe")
    ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        if (webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook is not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature", exception);
        }

        if (!"checkout.session.completed".equals(event.getType())
                && !"checkout.session.async_payment_succeeded".equals(event.getType())) {
            return ResponseEntity.noContent().build();
        }
        StripeObject data = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unable to parse Stripe checkout event"));
        if (!(data instanceof Session session)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected Stripe event payload");
        }
        if (!"payment".equals(session.getMode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected Stripe checkout mode");
        }
        if ("paid".equals(session.getPaymentStatus())) {
            orderService.markPaid(session);
        }
        return ResponseEntity.noContent().build();
    }
}
