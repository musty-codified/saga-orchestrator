package com.mustycodified.musty_notification_service.commonlib.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class RequestPayload implements Serializable {
	/** Serial version */
	private static final long serialVersionUID = 7868310611900741033L;

	private String productId;
	private BigDecimal totalPrice;
	private Integer quantity;
	private String status;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RequestPayload [");
		builder.append("productId=").append(productId).append(", ");
		builder.append("totalPrice=").append(totalPrice);
		builder.append("quantity=").append(quantity).append(", ");
		builder.append("status=").append(status);
		builder.append("]");
		return builder.toString();
	}


}
