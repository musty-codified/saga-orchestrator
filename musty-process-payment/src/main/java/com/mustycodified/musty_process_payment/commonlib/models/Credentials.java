package com.mustycodified.musty_process_payment.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Credentials implements Serializable {

	private static final long serialVersionUID = 7858293093666217131L;

	public Credentials() {
	}

	public Credentials(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	private String userName;
	private String password;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Credentials [userName=");
		builder.append(userName);
		builder.append(", password=");
		builder.append(password);
		builder.append("]");
		return builder.toString();
	}
}
