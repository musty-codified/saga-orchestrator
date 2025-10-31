package com.mustycodified.musty_create_order.models.consumer.response;

import com.google.gson.annotations.SerializedName;
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
public class ConsumerStatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 821324005757025788L;

    @SerializedName("message")
    private String message;

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BringStatus[")
                .append("message=").append(message)
                .append("]");
        return builder.toString();
    }
}
