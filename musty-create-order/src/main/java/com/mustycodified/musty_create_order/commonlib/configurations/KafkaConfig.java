package com.mustycodified.musty_create_order.commonlib.configurations;


import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Value("${camel.component.kafka.brokers}")
    private String kafkaBrokerUrl;

    @Bean
    public KafkaComponent kafkaComponent(CamelContext context) {
        KafkaComponent kafka = new KafkaComponent();
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setBrokers(kafkaBrokerUrl);
        kafka.setConfiguration(kafkaConfiguration);
        context.addComponent("kafka", kafka);
        return kafka;
    }

}
