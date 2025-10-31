package com.mustycodified.musty_create_order.commonlib.enums;

/**
 * zenz-common-libs
 * ErrorCodesEnum.java
 * Jun 27, 2023
 *
 * @author avazquez | Bring global - Zenz
 * @version  1.0.0
 * 
 */
public enum ErrorCodesEnum {

	EHF00("Success", "00"),
	EHF06("General Error", "06"),
	EHF63("Security violation", "63"),
	EHF92("Exception", "92"),
	EHF94("Duplicate ref", "94"),

	EHF1000("Processed Successfully", "EHF-1000"),
	EHF1002("General Error", "EHF-1001"),
	EHF1004("Target system unavailable", "EHF-1004"),
	EHF1013("Duplicate message identifier", "EHF-1013"),
	EHF1014("Message validation failed", "EHF-1014"),
	EHF1016("Request received and acknowledged", "EHF-1016"),
	EHF1024("Route not configured in HCV", "EHF-1024"),
	EHF1999("Unknown error", "EHF-1999"),
	EHF2013("Generic business error", "EHF-2013"),
	EHF2027("Target system not able to reply. Reconcilation advised", "EHF-2027"),
	EHF2047("Backend not available", "EHF-2047"),
	EHF2138("No condition match found. Request rejected", "EHF-2138");

	private String message;
	private String code;

	/**
	 * @param description
	 * @param code
	 */
	private ErrorCodesEnum(String message, String code) {
		this.message = message;
		this.code = code;
	}

	/**
	 * @return the description
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}
}
