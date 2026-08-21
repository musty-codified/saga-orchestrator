package com.mustycodified.musty_process_payment.commonlib.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

// @Component — disabled: legacy Zenz framework config, no active code references this bean
@ConfigurationProperties("zenz-ng-get-unit-of-measurements")
@Data
public class AppConfiguration {

    private HeaderValuesConfig headerValues;
    private String cred;


    @Data
    public static class HeaderValuesConfig {
        private String serviceCode;
        private String serviceName;
        private List<String> channelCode;
        private String channelName;
        private String routeCode;
        private String routeName;
        private String serviceMode;
        private String minorServiceVersion;
    }
}
