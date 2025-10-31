package com.mustycodified.musty_process_payment.commonlib.constants;


/**
 * zenz-common-libs
 * ConstantsCommons.java
 * Mar 24, 2022
 *
 * @author avazquez | Bring global - KCB
 * @version 1.0.0
 * 
 */
public class ConstantsCommons {

	private ConstantsCommons () {

	}

    public static final String ORIGINAL_REQUEST = "originalRequest";

	public static final String APP_REQUEST = "appRequest";//added for the the headerSetterProcessor
	public static final String APP_REQUEST_TYPE = "appRequestType";//added for the the headerSetterProcessor

	public static final String SLASH = "/";
	public static final String EMPTY = "";

	public static final String NO_PROVIDER_CREDENTIALS_PRESENT = "noProviderCredentialsPresent";
	public static final String VAULT_CONNECTOR_URI = "vaultConnectorUri";

	public static final String CALLER_IP = "clientIPAddress";
	public static final String CHANNEL_CODE_HEADER = "channelCode";

	public static final String SERVICE_SECURITY_DEFINITION_BASIC_VALUE = "Basic";
	public static final String SERVICE_AUTHORIZATION_HEADER_NAME = "Authorization";

	/****************** List User/Password *******************************/
	public static final String VAULT_USERNAME_HEADER = "vaultUser";
	public static final String VAULT_PASSWORD_HEADER = "vaultPassword";

	/****************** List User/Password *******************************/


	public static final String USERNAME_RESPONSE_HEADER_NAME = "username";
	public static final String PASSWORD_RESPONSE_HEADER_NAME = "password";
	public static final String REMOVE_HEADERS_PATTERN = "*";
	public static final String DEBUG_MESSAGE = "Provider credentials not found in local cache, invoking connector vault to get credentials";
    public static final String EXCHANGE_PROPERTY = "${exchangeProperty.";

	public static final String SERVICE_CODE = "serviceCode";

	public static final String CAMEL_PASSWORD = "camelPassword";

}
