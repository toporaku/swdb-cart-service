package com.cart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductDto {

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("gtin")
    private String gtin;

    @JsonProperty("product")
    private String product;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private Float price;

    @JsonProperty("stock")
    private Integer stock;

    @JsonProperty("category_id")
    private Integer categoryId;

    @JsonProperty("status")
    private Integer status;

    public ProductDto() {
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
