package com.mustycodified.musty_create_order.commonlib.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {

    @NotNull
    private String orderId;

    @NotNull
    private String uid;

    @NotNull
    private BigDecimal totalPrice;

    @NotNull
    private Integer quantity;

    @NotNull
    private String status;

    @Override
    public String toString() {
        return "OrderResponse [" +
                "id=" + orderId +
                "uid=" + uid + ", " +
                "totalPrice=" + totalPrice + ", " +
                "quantity=" + quantity + ", " +
                "status=" + status +
                "]";
    }

}


