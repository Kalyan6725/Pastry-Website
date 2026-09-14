package org.example.backend.service;

import org.example.backend.dto.cart.AddCartItemRequest;
import org.example.backend.dto.cart.CartItemResponse;
import org.example.backend.dto.cart.CartResponse;
import org.example.backend.dto.cart.UpdateCartItemRequest;
import org.example.backend.entity.Cart;
import org.example.backend.entity.CartItem;
import org.example.backend.entity.ProductVariant;
import org.example.backend.exception.BadRequestException;
import org.example.backend.exception.EmptyCartException;
import org.example.backend.exception.ForbiddenException;
import org.example.backend.exception.InactiveProductException;
import org.example.backend.exception.InactiveVariantException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.CartItemRepository;
import org.example.backend.repository.CartRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, UserRepository userRepository, ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productService = productService;
    }

    public CartResponse getMyCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItemResponse> items = cartItemRepository.findByCartId(cart.getId()).stream()
                .map(this::toResponse)
                .toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, subtotal);
    }

    @Transactional
    public CartItemResponse addItem(Long userId, AddCartItemRequest request) {
        if (request.quantity() == null || request.quantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        Cart cart = getOrCreateCart(userId);
        ProductVariant variant = productService.getProductVariantForCart(request.productVariantId());

        CartItem existing = cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId()).orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
            CartItem saved = cartItemRepository.save(existing);
            return toResponse(saved);
        }

        CartItem item = CartItem.builder().cart(cart).productVariant(variant).quantity(request.quantity()).build();
        return toResponse(cartItemRepository.save(item));
    }

    @Transactional
    public CartItemResponse updateItemQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        if (request.quantity() == null || request.quantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ForbiddenException("Cart item does not belong to current user");
        }
        item.setQuantity(request.quantity());
        return toResponse(cartItemRepository.save(item));
    }

    @Transactional
    public void removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ForbiddenException("Cart item does not belong to current user");
        }
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    public Cart getCartForCheckout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        if (cartItemRepository.findByCartId(cart.getId()).isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }
        return cart;
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            var user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    private CartItemResponse toResponse(CartItem item) {
        ProductVariant variant = item.getProductVariant();
        BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getFlavour() == null ? variant.getWeightInGrams() + "g" : variant.getFlavour() + " / " + variant.getWeightInGrams() + "g",
                item.getQuantity(),
                variant.getPrice(),
                lineTotal
        );
    }
}
