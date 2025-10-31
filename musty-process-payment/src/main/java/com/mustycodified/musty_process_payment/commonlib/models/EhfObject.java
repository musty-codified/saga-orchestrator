package com.mustycodified.musty_process_payment.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * account-info
 * EhfObject.java
 * Oct 31, 2022
 *
 * @author * @author avazquez | Bring global - Sabadell
 * @version 1.0.0
 * 
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EhfObject implements Serializable {

	private static final long serialVersionUID = -8551895066257437538L;
	private String key;
	private EhfRecord ehfRecord;

}
