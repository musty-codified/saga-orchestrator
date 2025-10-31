package com.mustycodified.musty_notification_service.commonlib.models;//package com.zenith.zenzngcreateunitofmeasurements.commonlib.models;

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
public class ResponseWrapper implements Serializable {
    /** Serial version UID */
    private static final long serialVersionUID = 3547641363206949943L;
    private ResponseHeader header;
    private ResponsePayload responsePayload;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ResponseWrapper [header=");
        builder.append(header);
        builder.append(", responsePayload=");
        builder.append(responsePayload);
        builder.append("]");
        return builder.toString();
    }

}