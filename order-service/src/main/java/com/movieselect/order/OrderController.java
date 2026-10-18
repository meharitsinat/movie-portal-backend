package com.movieselect.order;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.movieselect.order.OrderDtos.CreateOrderRequest;
import com.movieselect.order.OrderDtos.CheckoutResponse;
import com.movieselect.order.OrderDtos.OrderResponse;
import com.movieselect.order.OrderDtos.SetPriceRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class OrderController {
    private final OrderService orderService;
    private final CheckoutService checkoutService;

    OrderController(OrderService orderService, CheckoutService checkoutService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('GUEST')")
    ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {
        orderService.validateMovies(jwt.getTokenValue(), request);
        OrderResponse order = orderService.create(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("email"),
                request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('GUEST', 'ADMIN')")
    List<OrderResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return orderService.list(userId(jwt), isAdmin(jwt));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('GUEST', 'ADMIN')")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return orderService.get(id, userId(jwt), isAdmin(jwt));
    }

    @PostMapping("/orders/{id}/checkout")
    @PreAuthorize("hasRole('GUEST')")
    CheckoutResponse checkout(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return checkoutService.createCheckout(id, userId(jwt));
    }

    @PatchMapping("/admin/orders/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    OrderResponse setPrice(@PathVariable UUID id, @Valid @RequestBody SetPriceRequest request) {
        return orderService.setPrice(id, request.price());
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
