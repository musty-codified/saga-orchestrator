package com.mustycodified.commonlib.configurations;

import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBrokerUrl;

    @Bean
    public KafkaComponent kafkaComponent(CamelContext context) {
        KafkaComponent kafka = new KafkaComponent();
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setBrokers(kafkaBrokerUrl);
        kafkaConfiguration.setKeySerializer("org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfiguration.setValueSerializer("org.apache.kafka.common.serialization.StringSerializer");
        kafka.setConfiguration(kafkaConfiguration);
        context.addComponent("kafka", kafka);
        return kafka;
    }
}