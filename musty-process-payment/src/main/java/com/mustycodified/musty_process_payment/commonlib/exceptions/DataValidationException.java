package com.mustycodified.musty_process_payment.commonlib.exceptions;


/**
 * zenz-common-libs
 * DataValidationException.java
 * Jun 27, 2023
 *
 * @author avazquez | Bring global - Zenz
 * @version  1.0.0
 * 
 */
public class DataValidationException extends Exception {
	/** Serial version */
	private static final long serialVersionUID = 5984136394820807294L;

	public DataValidationException() {
		super();
	}

	public DataValidationException(String message) {
		super(message);
	}
}
