package com.cart.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cart.api.entity.CartItem;

@Repository
public interface RepoCartItem extends JpaRepository<CartItem, Integer> {

    @Query(value = "SELECT * FROM cart_item WHERE cart_id = :cartId", nativeQuery = true)
    List<CartItem> findByCartId(@Param("cartId") Integer cartId);

    @Query(value = "SELECT * FROM cart_item WHERE cart_id = :cartId AND gtin = :gtin", nativeQuery = true)
    Optional<CartItem> findByCartIdAndGtin(@Param("cartId") Integer cartId, @Param("gtin") String gtin);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cart_item WHERE cart_id = :cartId", nativeQuery = true)
    void deleteByCartId(@Param("cartId") Integer cartId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM cart_item WHERE id = :id", nativeQuery = true)
    void deleteByIdNative(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE cart_item SET quantity = :quantity WHERE id = :id", nativeQuery = true)
    void updateQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
