package com.mustycodified.musty_notification_service.models.provider.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 76140713799486170L;

    @SerializedName("status")
    String status;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ZenithResponse [ status=").append(status)
                .append("]");
        return builder.toString();
    }

}
