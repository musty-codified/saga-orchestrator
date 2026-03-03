package com.mustycodified.musty_process_payment.commonlib.configurations;


import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
