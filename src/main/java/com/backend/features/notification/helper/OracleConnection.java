package com.backend.features.notification.helper;

import com.backend.config.DataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Slf4j
public class OracleConnection {

    private DataSource oracleDataSource;
    private final DataSourceConfig.OracleDataSourceProperties oracleProps;

    public OracleConnection(DataSourceConfig.OracleDataSourceProperties oracleProps) {
        this.oracleProps = oracleProps;
    }

    @PostConstruct
    public synchronized void initialize() {
        if (!oracleProps.isEnabled()) {
            log.warn("Oracle datasource is disabled");
            return;
        }

        if (oracleDataSource != null) {
            log.debug("Oracle datasource already initialized");
            return;
        }

        log.info("Configuring SECONDARY datasource: Oracle");
        HikariDataSource hikariDataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(oracleProps.getUrl())
                .username(oracleProps.getUsername())
                .password(oracleProps.getPassword())
                .driverClassName(oracleProps.getDriverClassName())
                .build();

        hikariDataSource.setMaximumPoolSize(oracleProps.getMaximumPoolSize());
        hikariDataSource.setMinimumIdle(oracleProps.getMinimumIdle());
        hikariDataSource.setConnectionTimeout(oracleProps.getConnectionTimeout());
        hikariDataSource.setIdleTimeout(oracleProps.getIdleTimeout());
        hikariDataSource.setPoolName("OracleHikariPool");

        this.oracleDataSource = hikariDataSource;
        log.info("Oracle datasource initialized with HikariCP pool");
    }

    public Connection getConnection() throws SQLException {
        if (oracleDataSource == null) {
            throw new SQLException("Oracle DataSource is not initialized. Check datasource.oracle.enabled setting");
        }
        return oracleDataSource.getConnection();
    }
}
