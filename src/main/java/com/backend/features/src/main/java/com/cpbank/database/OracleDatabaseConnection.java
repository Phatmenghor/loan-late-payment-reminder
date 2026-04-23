package com.backend.features.src.main.java.com.cpbank.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OracleDatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(OracleDatabaseConnection.class);

    private static final String ORACLE_CLASS = "oracle.jdbc.driver.OracleDriver";
    private static final String ORACLE_URL = "jdbc:oracle:thin:@192.168.102.5:1521:dwh";
    private static final String ORACLE_USERNAME = "dwh";
    private static final String ORACLE_PASSWORD = "Bnk$$444";

    public static Connection getConnection() {
        try {
            Class.forName(ORACLE_CLASS);
            return DriverManager.getConnection(ORACLE_URL, ORACLE_USERNAME, ORACLE_PASSWORD);
        } catch (ClassNotFoundException e) {
            logger.error("Oracle Driver not found: {}", e.getMessage(), e);
            throw new RuntimeException("Oracle Driver not found", e);
        } catch (SQLException e) {
            logger.error("Connection error: {}", e.getMessage(), e);
            throw new RuntimeException("Connection error", e);
        }
    }
}