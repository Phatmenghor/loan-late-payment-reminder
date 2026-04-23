package com.backend.features.notification.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OracleHelper {

    private final DataSource oracleDataSource;

    public OracleHelper(@Qualifier("oracleDataSource") DataSource oracleDataSource) {
        this.oracleDataSource = oracleDataSource;
    }

    public List<String> selectPendingSmsPhoneNumbers() {
        List<String> phoneNumbers = new ArrayList<>();
        String query = "SELECT Tell FROM D_Cbs_Sms_Log_Test WHERE Sms_Status != ?";

        try (Connection con = oracleDataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, "SVC-SUCCESS-00");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String phone = rs.getString("Tell");
                phoneNumbers.add(phone);
                log.debug("Found pending SMS for phone: {}", phone);
            }

            log.info("Selected {} pending SMS from Oracle", phoneNumbers.size());
            return phoneNumbers;

        } catch (SQLException e) {
            log.error("Error fetching phone numbers from Oracle database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to select pending SMS from Oracle", e);
        }
    }

    public void updateSmsStatus(String phoneNumber, String status) {
        String query = "UPDATE D_Cbs_Sms_Log_Test SET Sms_Status = ?, Sms_Log_Dt = ? WHERE Tell = ?";

        try (Connection con = oracleDataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, phoneNumber);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                log.info("Oracle: Successfully updated SMS status for phone: {} | Status: {}", phoneNumber, status);
            } else {
                log.warn("Oracle: No records updated for phone: {}", phoneNumber);
            }

        } catch (SQLException e) {
            log.error("Oracle: Failed to update SMS status for phone: {} | Error: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to update SMS status in Oracle", e);
        }
    }
}
