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
    private String id;

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
        StringBuilder builder = new StringBuilder();
        builder.append("OrderResponse [");
        builder.append("id=").append(id);
        builder.append("uid=").append(uid).append(", ");
        builder.append("totalPrice=").append(totalPrice).append(", ");
        builder.append("quantity=").append(quantity).append(", ");
        builder.append("status=").append(status);
        builder.append("]");
        return builder.toString();
    }

}


