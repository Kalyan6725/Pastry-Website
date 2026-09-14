package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.cart.AddCartItemRequest;
import org.example.backend.dto.cart.CartResponse;
import org.example.backend.dto.cart.CartItemResponse;
import org.example.backend.dto.cart.UpdateCartItemRequest;
import org.example.backend.security.UserPrincipal;
import org.example.backend.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public CartResponse getMyCart(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getMyCart(principal.getId());
    }

    @PostMapping("/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartItemResponse addItem(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(principal.getId(), request);
    }

    @PutMapping("/cart/items/{id}")
    public CartItemResponse updateItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(principal.getId(), id, request);
    }

    @DeleteMapping("/cart/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        cartService.removeItem(principal.getId(), id);
    }
}
