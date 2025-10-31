package com.mustycodified.musty_notification_service.processors;


import com.mustycodified.musty_notification_service.commonlib.cache.ProviderCredentialsLocalCache;
import com.mustycodified.musty_notification_service.commonlib.configurations.AppConfiguration;
import com.mustycodified.musty_notification_service.commonlib.models.RequestWrapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.mustycodified.musty_notification_service.commonlib.constants.ConstantsCommons.*;


@Component
public class ProviderCredentialsCheckerProcessor implements Processor {

	private static final Logger logger = LoggerFactory.getLogger(ProviderCredentialsCheckerProcessor.class);

    private final AppConfiguration appConfiguration;
    private final ProviderCredentialsLocalCache providerCredentialsLocalCache;
    
    @Value("${vault-connector.uri}")
    private String vaultConnectorUri;
    
    @Value("${vault-connector.uri-params}")
    private String vaultConnectorUriParams;

    public ProviderCredentialsCheckerProcessor(AppConfiguration appConfiguration, ProviderCredentialsLocalCache providerCredentialsLocalCache) {
        this.appConfiguration = appConfiguration;
        this.providerCredentialsLocalCache = providerCredentialsLocalCache;
    }


    @Override
    public void process(Exchange exchange) throws Exception {
        RequestWrapper request = exchange.getProperty(ORIGINAL_REQUEST, RequestWrapper.class);

        Boolean providerCredentialsPresent = providerCredentialsLocalCache.isProviderCredentialsPresent();
        exchange.setProperty(NO_PROVIDER_CREDENTIALS_PRESENT, !providerCredentialsPresent);
        if (Boolean.FALSE.equals(providerCredentialsPresent)) {
            logger.debug(DEBUG_MESSAGE);
            String dynamicUri = vaultConnectorUri;
            dynamicUri += request.getHeader().getRouteCode();
            dynamicUri += SLASH;
//            dynamicUri += appConfiguration.getSendgrid().getApiKey();
            dynamicUri += vaultConnectorUriParams;
            logger.info("Credentials not found in local cache, fetching from vault :: {}", dynamicUri);
            exchange.setProperty(VAULT_CONNECTOR_URI, dynamicUri);
            exchange.getIn().removeHeaders(REMOVE_HEADERS_PATTERN,"conversationID","messageID","mockSuccessResponseFlag");
            exchange.getIn().setHeader(SERVICE_CODE, request.getHeader().getServiceCode());
            exchange.getIn().setBody(null);
        } else {
            String dynamicUri = vaultConnectorUri;
            dynamicUri += request.getHeader().getRouteCode();
            dynamicUri += SLASH;
//            dynamicUri += appConfiguration.getSendgrid().getApiKey();
            dynamicUri += vaultConnectorUriParams;
            logger.info("Credentials found in local cache:: {}", dynamicUri);
        }
    }

}
