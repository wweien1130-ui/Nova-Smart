package com.tianji.es.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LinkaiDataSourceConfig {

    @Value("${linkai.datasource.url}")
    private String url;

    @Value("${linkai.datasource.username}")
    private String username;

    @Value("${linkai.datasource.password}")
    private String password;

    @Value("${linkai.datasource.driver-class-name}")
    private String driverClassName;

    @Bean("linkaiDataSource")
    public DataSource linkaiDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setConnectionTimeout(5000);
        config.setMaximumPoolSize(5);
        config.setPoolName("LinkaiPool");
        return new HikariDataSource(config);
    }
}
