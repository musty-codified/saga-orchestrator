package com.mustycodified.musty_notification_service.commonlib.configurations;


import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Value("${camel.component.kafka.brokers:kafka:9092}")
    private String kafkaConnectUrl;

    @Bean
    public KafkaComponent kafkaComponent(CamelContext context) {
        KafkaComponent kafka = new KafkaComponent();

        // Set Kafka Configuration
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setBrokers(kafkaConnectUrl);

        kafka.setConfiguration(kafkaConfiguration);
        context.addComponent("kafka", kafka);

        return kafka;
    }
}
