package com.mustycodified.musty_create_order.processors;


import com.mustycodified.musty_create_order.commonlib.cache.ProviderCredentialsLocalCache;
import com.mustycodified.musty_create_order.commonlib.exceptions.NoProviderCredentialsException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.mustycodified.musty_create_order.commonlib.constants.ConstantsCommons.PASSWORD_RESPONSE_HEADER_NAME;
import static com.mustycodified.musty_create_order.commonlib.constants.ConstantsCommons.USERNAME_RESPONSE_HEADER_NAME;


@Component
public class ProviderCredentialsSetterProcessor implements Processor {

    @Autowired
    private ProviderCredentialsLocalCache providerCredentialsLocalCache;

    @Override
    public void process(Exchange exchange) throws Exception {
        String username = Optional.ofNullable(exchange.getIn().getHeader(USERNAME_RESPONSE_HEADER_NAME, String.class)).orElseThrow(NoProviderCredentialsException::new);
        String password = Optional.ofNullable(exchange.getIn().getHeader(PASSWORD_RESPONSE_HEADER_NAME, String.class)).orElseThrow(NoProviderCredentialsException::new);
        providerCredentialsLocalCache.setProviderCredential(username, password);
    }
}
