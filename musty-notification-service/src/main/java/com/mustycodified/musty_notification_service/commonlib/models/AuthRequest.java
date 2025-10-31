package com.mustycodified.musty_notification_service.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthRequest {

	private String routeCode;
	private String serviceCode;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AuthRequest [routeCode=");
		builder.append(routeCode);
		builder.append(", serviceCode=");
		builder.append(serviceCode);
		builder.append("]");
		return builder.toString();
	}
}