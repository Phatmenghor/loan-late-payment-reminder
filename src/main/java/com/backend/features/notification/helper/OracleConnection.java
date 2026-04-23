package com.backend.features.notification.helper;

import com.backend.config.DataSourceConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Slf4j
public class OracleConnection {

    private static DataSource oracleDataSource;
    private final DataSourceConfig.OracleDataSourceProperties oracleProps;

    public OracleConnection(DataSourceConfig.OracleDataSourceProperties oracleProps) {
        this.oracleProps = oracleProps;
    }

    @PostConstruct
    public void initialize() {
        if (!oracleProps.isEnabled()) {
            log.warn("Oracle datasource is disabled");
            return;
        }
        if (oracleDataSource == null) {
            log.info("Configuring SECONDARY datasource: Oracle");
            oracleDataSource = DataSourceBuilder.create()
                    .url(oracleProps.getUrl())
                    .username(oracleProps.getUsername())
                    .password(oracleProps.getPassword())
                    .driverClassName(oracleProps.getDriverClassName())
                    .build();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (oracleDataSource == null) {
            throw new SQLException("Oracle DataSource is not initialized");
        }
        return oracleDataSource.getConnection();
    }
}
