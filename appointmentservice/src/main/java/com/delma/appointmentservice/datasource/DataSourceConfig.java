package com.delma.appointmentservice.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.primary.jdbc-url}")
    private String primaryUrl;

    @Value("${spring.datasource.primary.username}")
    private String primaryUsername;

    @Value("${spring.datasource.primary.password}")
    private String primaryPassword;

    @Value("${spring.datasource.replica.jdbc-url}")
    private String replicaUrl;

    @Value("${spring.datasource.replica.username}")
    private String replicaUsername;

    @Value("${spring.datasource.replica.password}")
    private String replicaPassword;

    @Bean(name = "primaryDataSource")
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(primaryUrl);
        config.setUsername(primaryUsername);
        config.setPassword(primaryPassword);
        config.setPoolName("PrimaryPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        return new HikariDataSource(config);
    }

    @Bean(name = "replicaDataSource")
    public DataSource replicaDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(replicaUrl);
        config.setUsername(replicaUsername);
        config.setPassword(replicaPassword);
        config.setPoolName("ReplicaPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        return new HikariDataSource(config);
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(Map.of(
                DataSourceType.PRIMARY, primaryDataSource(),
                DataSourceType.REPLICA, replicaDataSource()
        ));
        routing.setDefaultTargetDataSource(primaryDataSource());
        routing.afterPropertiesSet();
        return routing;
    }
}