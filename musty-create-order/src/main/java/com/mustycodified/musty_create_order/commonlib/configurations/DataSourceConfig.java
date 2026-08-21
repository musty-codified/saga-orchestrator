package com.mustycodified.musty_create_order.commonlib.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Primary
    @Bean(name = "postgresDataSource")
    @ConfigurationProperties(prefix = "spring.postgres.datasource")
    public DataSource postgresDataSource() {
        return DataSourceBuilder.create().build();
    }
}