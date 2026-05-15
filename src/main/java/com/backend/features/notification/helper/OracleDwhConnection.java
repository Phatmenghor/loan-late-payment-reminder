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
public class OracleDwhConnection {

    private DataSource oracleDwhDataSource;
    private final DataSourceConfig.OracleDwhDataSourceProperties oracleDwhProps;

    public OracleDwhConnection(DataSourceConfig.OracleDwhDataSourceProperties oracleDwhProps) {
        this.oracleDwhProps = oracleDwhProps;
    }

    @PostConstruct
    public synchronized void initialize() {
        if (!oracleDwhProps.isEnabled()) {
            log.warn("Oracle DWH datasource is disabled");
            return;
        }

        if (oracleDwhDataSource != null) {
            log.debug("Oracle DWH datasource already initialized");
            return;
        }

        log.info("Configuring SECONDARY datasource: Oracle DWH");
        HikariDataSource hikariDataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(oracleDwhProps.getUrl())
                .username(oracleDwhProps.getUsername())
                .password(oracleDwhProps.getPassword())
                .driverClassName(oracleDwhProps.getDriverClassName())
                .build();

        hikariDataSource.setMaximumPoolSize(oracleDwhProps.getMaximumPoolSize());
        hikariDataSource.setMinimumIdle(oracleDwhProps.getMinimumIdle());
        hikariDataSource.setConnectionTimeout(oracleDwhProps.getConnectionTimeout());
        hikariDataSource.setIdleTimeout(oracleDwhProps.getIdleTimeout());
        hikariDataSource.setPoolName("OracleDwhHikariPool");

        this.oracleDwhDataSource = hikariDataSource;
        log.info("Oracle DWH datasource initialized with HikariCP pool");
    }

    public Connection getConnection() throws SQLException {
        if (oracleDwhDataSource == null) {
            throw new SQLException("Oracle DWH DataSource is not initialized. Check datasource.oracle-dwh.enabled setting");
        }
        return oracleDwhDataSource.getConnection();
    }
}
