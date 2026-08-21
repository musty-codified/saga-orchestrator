package com.mustycodified.musty_create_order.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponsePayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 353652223581858774L;
    private OrderResponse data;

    @Override
    public String toString() {
        return "ResponsePayload [success=" +
                ", data=" +
                data +
                "]";
    }

}