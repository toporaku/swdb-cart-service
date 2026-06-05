package com.cart;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.cart.api.dto.ProductDto;
import com.cart.api.entity.CartItem;
import com.cart.api.repository.RepoCartItem;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CartServiceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepoCartItem repo;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestTemplate restTemplate;

    @Value("${jwt.secret}")
    private String testSecret;

    private String customerTokenUser1;
    private String customerTokenUser2;
    private String adminToken;

    private final String activeGtin = "1234567890123";
    private final String inactiveGtin = "9876543210987";
    private final String notFoundGtin = "0000000000000";

    @BeforeEach
    void setup() {
        repo.deleteAll();

        // Generate tokens
        customerTokenUser1 = generateToken("customer1", 1, List.of("User"));
        customerTokenUser2 = generateToken("customer2", 2, List.of("User"));
        adminToken = generateToken("admin_user", 99, List.of("Administrator"));

        // Mock product-service for active product
        ProductDto activeProduct = new ProductDto();
        activeProduct.setGtin(activeGtin);
        activeProduct.setProduct("Smart TV");
        activeProduct.setPrice(499.99f);
        activeProduct.setStock(10);
        activeProduct.setStatus(1);

        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + activeGtin),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(ProductDto.class)
        )).thenReturn(new ResponseEntity<>(activeProduct, HttpStatus.OK));

        // Mock product-service for inactive product
        ProductDto inactiveProduct = new ProductDto();
        inactiveProduct.setGtin(inactiveGtin);
        inactiveProduct.setProduct("Old Radio");
        inactiveProduct.setPrice(19.99f);
        inactiveProduct.setStock(5);
        inactiveProduct.setStatus(0);

        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + inactiveGtin),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(ProductDto.class)
        )).thenReturn(new ResponseEntity<>(inactiveProduct, HttpStatus.OK));

        // Mock product-service for not found product
        Mockito.when(restTemplate.exchange(
                Mockito.eq("http://PRODUCT/product/gtin/" + notFoundGtin),
                Mockito.eq(HttpMethod.GET),
                Mockito.any(HttpEntity.class),
                Mockito.eq(ProductDto.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "El producto no existe"));
    }

    private String generateToken(String username, Integer id, List<String> roles) {
        Key key = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(username)
                .claim("id", id)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // @spec CART-EXT-001
    @Test
    void testAddToCartSuccess() throws Exception {
        CartItem item = new CartItem(1, activeGtin, 2);

        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated());

        List<CartItem> items = repo.findByCartId(1);
        assertEquals(1, items.size());
        assertEquals(activeGtin, items.get(0).getGtin());
        assertEquals(2, items.get(0).getQuantity());
    }

    // @spec CART-EXT-001
    @Test
    void testAddToCartAccumulatesQuantity() throws Exception {
        // Initial insert
        repo.save(new CartItem(1, activeGtin, 3));

        CartItem item = new CartItem(1, activeGtin, 4);

        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated());

        List<CartItem> items = repo.findByCartId(1);
        assertEquals(1, items.size());
        assertEquals(7, items.get(0).getQuantity());
    }

    // @spec CART-EXT-002
    @Test
    void testAddToCartExceedsStock() throws Exception {
        CartItem item = new CartItem(1, activeGtin, 11); // Stock is 10

        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());

        assertTrue(repo.findByCartId(1).isEmpty());
    }

    // @spec CART-EXT-002
    @Test
    void testAddToCartAccumulatesExceedsStock() throws Exception {
        repo.save(new CartItem(1, activeGtin, 8)); // Stock is 10

        CartItem item = new CartItem(1, activeGtin, 3); // Total 11 > 10

        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());

        List<CartItem> items = repo.findByCartId(1);
        assertEquals(1, items.size());
        assertEquals(8, items.get(0).getQuantity()); // Quantity shouldn't change
    }

    @Test
    void testAddToCartInactiveProduct() throws Exception {
        CartItem item = new CartItem(1, inactiveGtin, 1);

        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddToCartProductNotFound() throws Exception {
        CartItem item = new CartItem(1, notFoundGtin, 1);

        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isNotFound());
    }

    // @spec CART-EXT-003
    @Test
    void testGetCartItemsSuccess() throws Exception {
        repo.save(new CartItem(1, activeGtin, 2));

        mockMvc.perform(get("/cart-item/user/1")
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].gtin", is(activeGtin)))
                .andExpect(jsonPath("$[0].quantity", is(2)));
    }

    // @spec CART-EXT-004
    @Test
    void testDeleteCartItemSuccess() throws Exception {
        CartItem saved = repo.save(new CartItem(1, activeGtin, 2));

        mockMvc.perform(delete("/cart-item/" + saved.getId())
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isOk());

        assertTrue(repo.findById(saved.getId()).isEmpty());
    }

    // @spec CART-EXT-004
    @Test
    void testDeleteCartItemNotFound() throws Exception {
        mockMvc.perform(delete("/cart-item/9999")
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isNotFound());
    }

    // @spec CART-EXT-005
    @Test
    void testClearCartSuccess() throws Exception {
        repo.save(new CartItem(1, activeGtin, 2));
        repo.save(new CartItem(1, inactiveGtin, 1));

        mockMvc.perform(delete("/cart-item/user/1")
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isOk());

        assertTrue(repo.findByCartId(1).isEmpty());
    }

    // @spec CART-SEC-001
    @Test
    void testRejectRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/cart-item/user/1"))
                .andExpect(status().isUnauthorized());

        CartItem item = new CartItem(1, activeGtin, 2);
        mockMvc.perform(post("/cart-item")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isUnauthorized());
    }

    // @spec CART-SEC-002
    @Test
    void testDenyUserAccessingOtherCart() throws Exception {
        repo.save(new CartItem(2, activeGtin, 1));

        // User 1 trying to get User 2's cart -> Forbidden
        mockMvc.perform(get("/cart-item/user/2")
                .header("Authorization", "Bearer " + customerTokenUser1))
                .andExpect(status().isForbidden());

        // User 1 trying to add item to User 2's cart -> Forbidden
        CartItem item = new CartItem(2, activeGtin, 1);
        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + customerTokenUser1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isForbidden());
    }

    // @spec CART-SEC-003
    @Test
    void testAllowAdminAccessAnyCart() throws Exception {
        repo.save(new CartItem(1, activeGtin, 2));

        // Admin getting User 1's cart -> OK
        mockMvc.perform(get("/cart-item/user/1")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Admin adding item to User 1's cart -> OK (Created)
        CartItem item = new CartItem(1, activeGtin, 1);
        mockMvc.perform(post("/cart-item")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated());
    }
}
