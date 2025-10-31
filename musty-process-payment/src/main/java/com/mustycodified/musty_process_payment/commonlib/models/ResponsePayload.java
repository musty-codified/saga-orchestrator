package com.mustycodified.musty_process_payment.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponsePayload implements Serializable {

    private static final long serialVersionUID = 353652223581858774L;

    private ProductResponse data;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResponsePayload [success=");
        builder.append(", data=");
        builder.append(data);
        builder.append("]");
        return builder.toString();
    }

}