package org.example.backend;

import org.example.backend.dto.address.AddressRequest;
import org.example.backend.dto.auth.LoginRequest;
import org.example.backend.dto.auth.RegisterRequest;
import org.example.backend.dto.cart.AddCartItemRequest;
import org.example.backend.dto.category.CategoryRequest;
import org.example.backend.dto.product.ProductRequest;
import org.example.backend.dto.product.ProductVariantRequest;
import org.example.backend.entity.Category;
import org.example.backend.entity.Product;
import org.example.backend.entity.ProductVariant;
import org.example.backend.entity.User;
import org.example.backend.enums.EggType;
import org.example.backend.exception.DuplicateResourceException;
import org.example.backend.exception.UnauthorizedException;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.ProductRepository;
import org.example.backend.repository.ProductVariantRepository;
import org.example.backend.repository.UserRepository;
import org.example.backend.service.AddressService;
import org.example.backend.service.AuthService;
import org.example.backend.service.CartService;
import org.example.backend.service.CategoryService;
import org.example.backend.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ApplicationLayerIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private AuthService authService;
    @Autowired private CategoryService categoryService;
    @Autowired private ProductService productService;
    @Autowired private AddressService addressService;
    @Autowired private CartService cartService;

    @BeforeEach
    void setUp() {
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void customerRegistrationAndLoginWorks() {
        authService.register(new RegisterRequest("Alice", "alice@example.com", "password123"));

        var auth = authService.login(new LoginRequest("alice@example.com", "password123"));

        assertNotNull(auth.token());
        assertEquals("Alice", auth.user().name());
        assertEquals("alice@example.com", auth.user().email());
    }

    @Test
    void duplicateEmailRejected() {
        authService.register(new RegisterRequest("Alice", "alice@example.com", "password123"));

        assertThrows(DuplicateResourceException.class,
                () -> authService.register(new RegisterRequest("Alice 2", "alice@example.com", "password456")));
    }

    @Test
    void invalidLoginIsRejected() {
        authService.register(new RegisterRequest("Alice", "alice@example.com", "password123"));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("alice@example.com", "wrongpassword")));
    }

    @Test
    void priceFilterAndCategoryFilterWorkAtDatabaseLevel() {
        Category cake = categoryRepository.save(Category.builder().name("Cakes").description("Desserts").active(true).build());
        Product product = productRepository.save(Product.builder().name("Chocolate Cake").description("Rich dessert").imageUrl("https://img").category(cake).active(true).build());
        productVariantRepository.save(ProductVariant.builder().product(product).weightInGrams(500).flavour("Chocolate").eggType(EggType.EGGLESS).price(new BigDecimal("400.00")).active(true).build());
        productVariantRepository.save(ProductVariant.builder().product(product).weightInGrams(1000).flavour("Chocolate").eggType(EggType.EGGLESS).price(new BigDecimal("700.00")).active(true).build());

        var page = productService.browseCustomerCatalog(null, cake.getId(), null, new BigDecimal("500.00"), "price_asc", PageRequest.of(0, 20));
        assertEquals(1, page.getTotalElements());
        assertEquals("Chocolate Cake", page.getContent().get(0).name());

        var page2 = productService.browseCustomerCatalog("chocolate", null, null, null, "name_asc", PageRequest.of(0, 20));
        assertEquals(1, page2.getTotalElements());
    }

    @Test
    void cartDuplicateVariantIncrementsQuantity() {
        User user = userRepository.save(User.builder().name("Bob").email("bob@example.com").password("pw").role(org.example.backend.enums.Role.CUSTOMER).active(true).build());
        Category cake = categoryRepository.save(Category.builder().name("Cakes").active(true).build());
        Product product = productRepository.save(Product.builder().name("Cake").imageUrl("https://img").category(cake).active(true).build());
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder().product(product).weightInGrams(500).flavour("Vanilla").eggType(EggType.EGGLESS).price(new BigDecimal("120.00")).active(true).build());

        cartService.addItem(user.getId(), new AddCartItemRequest(variant.getId(), 1));
        cartService.addItem(user.getId(), new AddCartItemRequest(variant.getId(), 2));

        var cart = cartService.getMyCart(user.getId());
        assertEquals(1, cart.items().size());
        assertEquals(3, cart.items().get(0).quantity());
    }

    @Test
    void defaultAddressRuleEnforced() {
        User user = userRepository.save(User.builder().name("Cara").email("cara@example.com").password("pw").role(org.example.backend.enums.Role.CUSTOMER).active(true).build());
        var a = addressService.addAddress(user.getId(), new AddressRequest("Home", "9999999999", "A", null, "City", "State", "111111", "IN", true));
        var b = addressService.addAddress(user.getId(), new AddressRequest("Office", "8888888888", "B", null, "City", "State", "222222", "IN", true));

        assertFalse(addressService.getMyAddresses(user.getId()).stream().filter(x -> x.id().equals(a.id())).findFirst().orElseThrow().isDefault());
        assertTrue(addressService.getMyAddresses(user.getId()).stream().filter(x -> x.id().equals(b.id())).findFirst().orElseThrow().isDefault());
    }
}
