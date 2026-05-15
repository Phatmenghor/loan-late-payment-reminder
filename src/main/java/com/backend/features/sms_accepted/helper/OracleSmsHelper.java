package com.backend.features.sms_accepted.helper;

import com.backend.features.notification.helper.OracleConnection;
import com.backend.features.sms_accepted.dto.OracleSmsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OracleSmsHelper {

    private final OracleConnection oracleConnection;

    public List<OracleSmsDto> selectSmsPendingRecords() {
        List<OracleSmsDto> records = new ArrayList<>();
        String query = "SELECT msg_id, phone, message FROM VIEW_SMS WHERE status = 'PENDING'";

        try (Connection con = oracleConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                OracleSmsDto record = OracleSmsDto.builder()
                        .msgId(rs.getString("msg_id"))
                        .phone(rs.getString("phone"))
                        .message(rs.getString("message"))
                        .build();
                records.add(record);
            }

            log.info("Oracle: Selected {} pending SMS records from VIEW_SMS", records.size());
            return records;

        } catch (SQLException e) {
            log.error("Oracle: Error fetching SMS records from VIEW_SMS | Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to select SMS records from Oracle", e);
        }
    }

    public void updateSmsStatus(String msgId, String status) {
        String query = "UPDATE VIEW_SMS SET status = ? WHERE msg_id = ?";

        try (Connection con = oracleConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setString(2, msgId);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                log.info("Oracle: Updated SMS status to '{}' for msg_id: {}", status, msgId);
            } else {
                log.warn("Oracle: No records updated for msg_id: {}", msgId);
            }

        } catch (SQLException e) {
            log.error("Oracle: Error updating SMS status | msg_id: {}, Error: {}", msgId, e.getMessage(), e);
            throw new RuntimeException("Failed to update SMS status in Oracle", e);
        }
    }
}
