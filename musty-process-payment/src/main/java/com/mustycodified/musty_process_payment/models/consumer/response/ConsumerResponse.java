package com.mustycodified.musty_process_payment.models.consumer.response;

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
public class ConsumerResponse implements Serializable {


    @Serial
    private static final long serialVersionUID = 7737070392069647700L;

    private ConsumerStatus status;

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BringResponse[")
                .append("status=").append(status).append("]");
        return builder.toString();
    }
}
