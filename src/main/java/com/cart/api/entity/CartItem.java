package com.cart.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "El id del carrito es obligatorio")
    @Column(name = "cart_id")
    private Integer cartId;

    @NotNull(message = "El gtin es obligatorio")
    @Column(name = "gtin")
    private String gtin;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    @Column(name = "quantity")
    private Integer quantity;

    public CartItem() {
    }

    public CartItem(Integer cartId, String gtin, Integer quantity) {
        this.cartId = cartId;
        this.gtin = gtin;
        this.quantity = quantity;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCartId() {
        return cartId;
    }

    public void setCartId(Integer cartId) {
        this.cartId = cartId;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "id=" + id +
                ", cartId=" + cartId +
                ", gtin='" + gtin + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
