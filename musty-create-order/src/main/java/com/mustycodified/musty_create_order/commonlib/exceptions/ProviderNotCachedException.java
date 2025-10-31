package com.mustycodified.musty_create_order.commonlib.exceptions;

import lombok.NoArgsConstructor;

/**
 * zenz-common-libs
 * ProviderNotCachedException.java
 * Jun 27, 2023
 *
 * @author avazquez | Bring global - Zenz
 * @version  1.0.0
 * 
 */
@NoArgsConstructor
public class ProviderNotCachedException extends Exception {

    private static final long serialVersionUID = 5819281122008560712L;

    public ProviderNotCachedException(String message) {
        super(message);
    }

}
