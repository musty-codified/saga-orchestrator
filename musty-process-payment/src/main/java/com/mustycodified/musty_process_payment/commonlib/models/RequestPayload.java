package com.mustycodified.musty_process_payment.commonlib.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class RequestPayload implements Serializable {
	/** Serial version */
	private static final long serialVersionUID = 7868310611900741033L;

	private String productName;
	private String unitOfMeasurement;
	private BigDecimal unitPrice;
	private Integer stockAvailable;
	private String productImage;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RequestPayload [");
		builder.append("productName=").append(productName).append(", ");
		builder.append("unitOfMeasurement=").append(unitOfMeasurement).append(", ");
		builder.append("unitPrice=").append(unitPrice);
		builder.append("stockAvailable=").append(stockAvailable).append(", ");
		builder.append("productImage=").append(productImage);
		builder.append("]");
		return builder.toString();
	}


}
