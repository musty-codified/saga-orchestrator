package com.mustycodified.musty_create_order.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * account-info
 * EhfInfo.java
 * May 8, 2023
 *
 * @author avazquez | Bring global - Sabadell
 * @version 1.0.0
 * 
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EhfInfo implements Serializable {
	/** Serial version UID */
	private static final long serialVersionUID = -8647643527147121819L;
	private List<Item> item;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EhfInfo [item=");
		builder.append(item);
		builder.append("]");
		return builder.toString();
	}

}