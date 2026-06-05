package com.cart.api.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cart.api.entity.CartItem;
import com.cart.api.service.SvcCartItem;

@Tag(name = "Cart Item", description = "Gestión de elementos del carrito")
@RestController
@RequestMapping("/cart-item")
public class CtrlCartItem {

    @Autowired
    private SvcCartItem svc;

    // @spec CART-EXT-003, CART-INT-001
    @GetMapping("/user/{cartId}")
    public ResponseEntity<List<CartItem>> getCartItems(@PathVariable Integer cartId) {
        return svc.getCartItems(cartId);
    }

    // @spec CART-EXT-001, CART-EXT-002
    @PostMapping
    public ResponseEntity<String> addToCart(@Valid @RequestBody CartItem item) {
        return svc.addToCart(item);
    }

    // @spec CART-EXT-004
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCartItem(@PathVariable Integer id) {
        return svc.deleteCartItem(id);
    }

    // @spec CART-EXT-005, CART-INT-002
    @DeleteMapping("/user/{cartId}")
    public ResponseEntity<String> clearCart(@PathVariable Integer cartId) {
        return svc.clearCart(cartId);
    }
}
