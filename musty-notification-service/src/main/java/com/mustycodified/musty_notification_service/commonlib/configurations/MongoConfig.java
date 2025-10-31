package com.mustycodified.musty_notification_service.commonlib.configurations;

import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;

public class MongoConfig {

    @Bean("mongoBean")
    public MongoClient mongoBean(MongoClient springMongoClient) {
        return springMongoClient;
    }
}
//spring.data.mongodb.uri=mongodb+srv://ilemonamustapha_db_user:UhEtvIBacDmduOPP@musty-cluster.b8cyyjp.mongodb.net/notifications?retryWrites=true&w=majority&appName=Musty-cluster