package com.mustycodified.musty_inventory_service.commonlib.configurations;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
public class InventoryTransactionConfig {

    @Bean
    public PlatformTransactionManager mySqlTransactionManager(@Qualifier("mySqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate mySqlJdbcTemplate(@Qualifier("mySqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public TransactionTemplate mySqlTransactionTemplate(PlatformTransactionManager mySqlTransactionManager) {
        return new TransactionTemplate(mySqlTransactionManager);
    }
}
