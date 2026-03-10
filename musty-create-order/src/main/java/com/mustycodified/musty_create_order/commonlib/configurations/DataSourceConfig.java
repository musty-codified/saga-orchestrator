package com.mustycodified.musty_create_order.commonlib.configurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSourceConfig {

    @Primary
    @Bean(name = "mySqlDataSource")
    @ConfigurationProperties(prefix = "spring.mysql.datasource")
    public DataSource setupMySqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "postgresDataSource")
    @ConfigurationProperties(prefix = "spring.postgres.datasource")
    public DataSource setupPgDataSource() {
        return DataSourceBuilder.create().build();

    }
}