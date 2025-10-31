package com.mustycodified.musty_create_order.commonlib.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class RequestPayload implements Serializable {
	/** Serial version */
	@Serial
	private static final long serialVersionUID = 7868310611900741033L;

	private BigDecimal totalPrice;
	private Integer quantity;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RequestPayload [");
		builder.append("totalPrice=").append(totalPrice);
		builder.append("quantity=").append(quantity).append(", ");
		builder.append("]");
		return builder.toString();
	}


}
