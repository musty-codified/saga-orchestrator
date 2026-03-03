package com.mustycodified.musty_notification_service.commonlib.configurations;

import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;


public class MongoConfig {

    @Bean("mongoBean")
    public MongoClient mongoBean(MongoClient springMongoClient) {
        return springMongoClient;
    }
}
