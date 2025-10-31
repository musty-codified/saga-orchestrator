package com.mustycodified.musty_notification_service.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * account-info
 * EhfResponse.java
 * Nov 1, 2022
 *
 * @author * @author avazquez | Bring global - Sabadell
 * @version 1.0.0
 * 
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EhfResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5581964837224953052L;
	private Boolean success;
	private String detail;
	private List<EhfObject> records;

}