package com.backend.features.notification.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Slf4j
public class OracleConnection {

    private static DataSource oracleDataSource;

    public OracleConnection(@Qualifier("oracleDataSource") DataSource dataSource) {
        OracleConnection.oracleDataSource = dataSource;
    }

    public static Connection getConnection() throws SQLException {
        if (oracleDataSource == null) {
            throw new SQLException("Oracle DataSource is not initialized");
        }
        return oracleDataSource.getConnection();
    }
}
