package com.mustycodified.musty_create_order.commonlib.configurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
    public static DataSource setupMySqlDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/db_create_order");
        dataSource.setUsername("root");
        dataSource.setPassword("papimaciano");
        return dataSource;
    }

    @Bean(name = "postgresDataSource")
    @ConfigurationProperties(prefix = "spring.postgres.datasource")
    public static DataSource setupPgDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/orders_db");
        dataSource.setUsername("postgres");
        dataSource.setPassword("papi");
        return dataSource;

    }
}