package com.cart.api.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.cart.api.entity.CartItem;

public interface SvcCartItem {

    // @spec CART-EXT-003, CART-INT-001
    ResponseEntity<List<CartItem>> getCartItems(Integer cartId);

    // @spec CART-EXT-001, CART-EXT-002
    ResponseEntity<String> addToCart(CartItem item);

    // @spec CART-EXT-004
    ResponseEntity<String> deleteCartItem(Integer id);

    // @spec CART-EXT-005, CART-INT-002
    ResponseEntity<String> clearCart(Integer cartId);
}
