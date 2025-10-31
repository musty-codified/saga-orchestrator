package com.mustycodified.musty_process_payment.commonlib.configurations;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RestConfiguration extends RouteBuilder {

    @Value("${camel.rest.component:servlet}")
    private String camelComponent;

    @Value("${swagger.api.path:/api}")
    private String apiPath;

    @Value("${swagger.api.enableCors:true}")
    private Boolean apiEnableCors;

    @Value("${swagger.api-docs.path:/api-docs}")
    private String apiDocsPath;

    @Value("${swagger.api-docs.version:1.0.0}")
    private String apiDocsVersion;

    @Value("${camel.springboot.name}")
    private String apiDocsTitle;

    @Autowired
    private Environment env;

    @Value("${camel.servlet.mapping.context-path}")
    private String contextPath;

    @Override
    public void configure() throws Exception {

        // Rest Configuration
        restConfiguration()
                .component(camelComponent)
                .contextPath(apiPath)
                .enableCORS(apiEnableCors)
                .corsHeaderProperty("Access-Control-Allow-Methods", "POST")
                .port(env.getProperty("server.port", "9080"))
                .contextPath(contextPath.substring(0, contextPath.length() - 2))
                .apiContextPath(apiDocsPath)
                .apiProperty("api.title", apiDocsTitle)
                .apiProperty("api.version", apiDocsVersion)
                .apiProperty("cors", apiEnableCors.toString())
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("prettyPrint", "true");


    }
}