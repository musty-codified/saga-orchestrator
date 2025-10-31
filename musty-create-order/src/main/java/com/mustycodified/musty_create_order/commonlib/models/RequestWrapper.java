package com.mustycodified.musty_create_order.commonlib.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class RequestWrapper implements Serializable {
    /** Serial version UID */
    private static final long serialVersionUID = 6464903968440189419L;

    @JsonIgnore
    private RequestHeader header;
    private RequestPayload requestPayload;


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RequestWrapper [header=");
        builder.append(header);
        builder.append(", requestPayload=");
        builder.append(requestPayload);
        builder.append("]");
        return builder.toString();
    }

}