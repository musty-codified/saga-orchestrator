package com.mustycodified.musty_notification_service.commonlib.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * account-info
 * EhfRecord.java
 * Oct 31, 2022
 *
 * @author * @author avazquez | Bring global - Sabadell
 * @version 1.0.0
 * 
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EhfRecord implements Serializable {

    private static final long serialVersionUID = -1317559232336740643L;
    private String statusCode;
    private String statusDescription;
    private String businessDescription;
    private String ehfRef;
    private String ehfDesc;
}
