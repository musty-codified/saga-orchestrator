package com.mustycodified.commonlib.enums;

import lombok.Getter;

@Getter
public enum HTTPCommonHeadersEnum {
    ACCEPT("Accept"),
    CONTENT_TYPE("Content-Type"),
    AUTHORIZATION("Authorization"),
    WWW_AUTHENTICATE("WWW-Authenticate"),
    PAYSTACK_VERIFICATION("x-paystack-signature");



    private final String name;

    HTTPCommonHeadersEnum(String name) { this.name = name; }

}