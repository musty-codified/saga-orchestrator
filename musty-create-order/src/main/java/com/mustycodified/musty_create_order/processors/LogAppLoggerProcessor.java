package com.mustycodified.musty_create_order.processors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.mustycodified.musty_create_order.commonlib.constants.ConstantsCommons;
import com.mustycodified.musty_create_order.commonlib.models.AppLogger;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogAppLoggerProcessor implements Processor {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter prettyPrintWriter;
    private static final Logger log = LoggerFactory.getLogger(LogAppLoggerProcessor.class);

    static {
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        prettyPrintWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }
    @Override
    public void process(Exchange exchange) throws Exception {
        AppLogger appLogger = exchange.getProperty(ConstantsCommons.APP_REQUEST, AppLogger.class);
        String requestType = exchange.getProperty(ConstantsCommons.APP_REQUEST_TYPE, String.class);
        String jsonLog = convertToJson(appLogger);
        // Mask passwords/credentials in the log message
        jsonLog = maskPasswords(jsonLog);
        log.info(requestType , jsonLog);
    }

    private String convertToJson(AppLogger appLogger) {
        try {
            return prettyPrintWriter.writeValueAsString(appLogger);
        } catch (JsonProcessingException e) {
            return "{}"; // Return an empty JSON object in case of an error
        }
    }
    private String maskPasswords(String logMessage) {
        // Define a regular expression to match and replace passwords/credentials
        String passwordRegex = "(\"camelPassword\"\\s*:\\s*\")[^\"]+";

        // Mask passwords/credentials with asterisks

        return logMessage.replaceAll(passwordRegex, "$1*****");
    }
}