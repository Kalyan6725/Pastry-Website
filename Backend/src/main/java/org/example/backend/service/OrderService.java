package org.example.backend.service;

import org.example.backend.dto.order.OrderRequest;
import org.example.backend.dto.order.OrderResponse;
import org.example.backend.dto.order.OrderStatusRequest;
import org.example.backend.dto.order.OrderItemResponse;
import org.example.backend.entity.Address;
import org.example.backend.entity.Cart;
import org.example.backend.entity.CartItem;
import org.example.backend.entity.Order;
import org.example.backend.entity.OrderItem;
import org.example.backend.entity.ProductVariant;
import org.example.backend.entity.User;
import org.example.backend.enums.OrderStatus;
import org.example.backend.exception.BadRequestException;
import org.example.backend.exception.EmptyCartException;
import org.example.backend.exception.ForbiddenException;
import org.example.backend.exception.InvalidOrderStatusException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.AddressRepository;
import org.example.backend.repository.CartItemRepository;
import org.example.backend.repository.OrderRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, AddressRepository addressRepository,
                        CartItemRepository cartItemRepository, CartService cartService,
                        UserRepository userRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.addressRepository = addressRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request) {
        Cart cart = cartService.getCartForCheckout(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        Address address = addressRepository.findByUserIdAndId(userId, request.addressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            if (!variant.isActive()) {
                throw new BadRequestException("Variant is inactive: " + variant.getId());
            }
            if (!variant.getProduct().isActive()) {
                throw new BadRequestException("Product is inactive: " + variant.getProduct().getId());
            }
            if (!variant.getProduct().getCategory().isActive()) {
                throw new BadRequestException("Category is inactive: " + variant.getProduct().getCategory().getId());
            }
            if (cartItem.getQuantity() < 1) {
                throw new BadRequestException("Cart item quantity must be at least 1");
            }
            subtotal = subtotal.add(variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        BigDecimal deliveryFee = subtotal.compareTo(new BigDecimal("1000.00")) >= 0 ? BigDecimal.ZERO : new BigDecimal("79.00");
        BigDecimal totalAmount = subtotal.add(deliveryFee);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(user)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .deliveryPhone(address.getPhone())
                .deliveryAddressLine1(address.getAddressLine1())
                .deliveryAddressLine2(address.getAddressLine2())
                .deliveryCity(address.getCity())
                .deliveryState(address.getState())
                .deliveryPostalCode(address.getPostalCode())
                .deliveryCountry(address.getCountry())
                .build();

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getProductVariant();
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(variant)
                    .productName(variant.getProduct().getName())
                    .weightInGrams(variant.getWeightInGrams())
                    .flavour(variant.getFlavour())
                    .eggType(variant.getEggType())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(variant.getPrice())
                    .build();
            savedOrder.getItems().add(orderItem);
        }

        orderRepository.save(savedOrder);
        paymentService.createPaymentForOrder(savedOrder.getId());
        cartItemRepository.deleteByCartId(cart.getId());

        return toResponse(savedOrder);
    }

    public Page<OrderResponse> getCustomerOrders(Long userId, Pageable pageable) {
        return orderRepository.findByCustomerId(userId, pageable).map(this::toResponse);
    }

    public OrderResponse getCustomerOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStatusException("Only placed or confirmed orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusRequest request) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateTransition(order.getStatus(), request.status());
        order.setStatus(request.status());
        return toResponse(orderRepository.save(order));
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        Map<OrderStatus, Set<OrderStatus>> allowed = Map.of(
                OrderStatus.PLACED, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
                OrderStatus.PREPARING, Set.of(OrderStatus.READY),
                OrderStatus.READY, Set.of(OrderStatus.OUT_FOR_DELIVERY),
                OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED),
                OrderStatus.DELIVERED, Set.of(),
                OrderStatus.CANCELLED, Set.of()
        );
        if (!allowed.getOrDefault(current, Set.of()).contains(next)) {
            throw new InvalidOrderStatusException("Invalid order status transition from " + current + " to " + next);
        }
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getDeliveryAddressLine1(),
                order.getDeliveryCity(),
                order.getDeliveryPhone(),
                order.getCreatedAt(),
                order.getItems().stream().map(this::toResponse).toList()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getWeightInGrams(),
                item.getFlavour(),
                item.getEggType(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }
}
