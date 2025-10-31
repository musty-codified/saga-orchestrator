package com.mustycodified.musty_process_payment.commonlib.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * bpr-customer-details
 * PrimaryData.java
 * Sep 4, 2022
 *
 * @author avazquez | Bring global - KCB
 * @version 1.0.0
 * 
 */
@Getter
@Setter
public class PrimaryData implements Serializable {

	/** Serial version UID */
	@Serial
	private static final long serialVersionUID = -8478984558316046120L;

	private String businessKey;
	private String businessKeyType;

	public PrimaryData() {
		//Unused constructor
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PrimaryData [businessKey=");
		builder.append(businessKey);
		builder.append(", businessKeyType=");
		builder.append(businessKeyType);
		builder.append("]");
		return builder.toString();
	}
}
