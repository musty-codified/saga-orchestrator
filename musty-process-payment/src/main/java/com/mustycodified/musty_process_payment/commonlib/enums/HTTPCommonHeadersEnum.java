package com.mustycodified.musty_process_payment.commonlib.enums;

/**
 * zenz-common-libs
 * HTTPCommonHeadersEnum.java
 * Jun 27, 2023
 *
 * @author avazquez | Bring global - Zenz
 * @version  1.0.0
 * 
 */
public enum HTTPCommonHeadersEnum {
	ACCEPT("Accept"),
	CONTENT_TYPE("Content-Type"),
	AUTHORIZATION("Authorization"),
	WWW_AUTHENTICATE("WWW-Authenticate");

	private String name;

	private HTTPCommonHeadersEnum(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}