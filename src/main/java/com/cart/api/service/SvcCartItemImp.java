package com.cart.api.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cart.api.dto.ProductDto;
import com.cart.api.entity.CartItem;
import com.cart.api.repository.RepoCartItem;
import com.cart.exception.ApiException;
import com.cart.exception.DBAccessException;

@Service
public class SvcCartItemImp implements SvcCartItem {

    @Autowired
    private RepoCartItem repo;

    @Autowired
    private RestTemplate restTemplate;

    private void validateUserAccess(Integer requestedCartId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN".equals(a.getAuthority()));

        Object credentials = auth.getCredentials();
        Integer tokenUserId = null;
        if (credentials instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) credentials;
            Object idObj = map.get("id");
            if (idObj instanceof Number) {
                tokenUserId = ((Number) idObj).intValue();
            }
        }

        // @spec CART-SEC-002, CART-SEC-003
        if (!isAdmin && (tokenUserId == null || !tokenUserId.equals(requestedCartId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para acceder a este recurso");
        }
    }

    private ProductDto getProductDetails(String gtin) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String authHeader = null;
        if (attributes != null) {
            authHeader = attributes.getRequest().getHeader("Authorization");
        }

        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ProductDto> response = restTemplate.exchange(
                    "http://PRODUCT/product/gtin/" + gtin,
                    HttpMethod.GET,
                    entity,
                    ProductDto.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ApiException(HttpStatus.NOT_FOUND, "El producto no existe");
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Error al validar el producto en el servicio de productos");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Error al validar el producto en el servicio de productos");
        }
    }

    // @spec CART-EXT-003, CART-INT-001
    @Override
    public ResponseEntity<List<CartItem>> getCartItems(Integer cartId) {
        validateUserAccess(cartId);
        try {
            List<CartItem> items = repo.findByCartId(cartId);
            return new ResponseEntity<>(items, HttpStatus.OK);
        } catch (DataAccessException e) {
            throw new DBAccessException(e);
        }
    }

    // @spec CART-EXT-001, CART-EXT-002
    @Override
    public ResponseEntity<String> addToCart(CartItem item) {
        if (item.getCartId() == null || item.getGtin() == null || item.getQuantity() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parámetros del carrito incompletos");
        }
        if (item.getQuantity() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor a cero");
        }
        validateUserAccess(item.getCartId());

        // Validate product exists and has sufficient stock
        ProductDto product = getProductDetails(item.getGtin());
        if (product == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "El producto no existe");
        }
        if (product.getStatus() != null && product.getStatus() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El producto no está activo");
        }

        try {
            Optional<CartItem> existingItemOpt = repo.findByCartIdAndGtin(item.getCartId(), item.getGtin());
            int targetQuantity = item.getQuantity();
            if (existingItemOpt.isPresent()) {
                targetQuantity += existingItemOpt.get().getQuantity();
            }

            // Check if quantity exceeds stock
            if (targetQuantity > product.getStock()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "La cantidad solicitada excede el stock disponible");
            }

            if (existingItemOpt.isPresent()) {
                repo.updateQuantity(existingItemOpt.get().getId(), targetQuantity);
            } else {
                repo.save(item);
            }

            return new ResponseEntity<>("El producto ha sido agregado al carrito", HttpStatus.CREATED);
        } catch (DataAccessException e) {
            throw new DBAccessException(e);
        }
    }

    // @spec CART-EXT-004
    @Override
    public ResponseEntity<String> deleteCartItem(Integer id) {
        try {
            CartItem item = repo.findById(id)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El elemento del carrito no existe"));
            
            // Only the owner of the cart or admin can delete
            validateUserAccess(item.getCartId());
            
            repo.deleteByIdNative(id);
            return new ResponseEntity<>("El producto ha sido eliminado del carrito", HttpStatus.OK);
        } catch (DataAccessException e) {
            throw new DBAccessException(e);
        }
    }

    // @spec CART-EXT-005, CART-INT-002
    @Override
    public ResponseEntity<String> clearCart(Integer cartId) {
        validateUserAccess(cartId);
        try {
            repo.deleteByCartId(cartId);
            return new ResponseEntity<>("El carrito ha sido limpiado", HttpStatus.OK);
        } catch (DataAccessException e) {
            throw new DBAccessException(e);
        }
    }
}
