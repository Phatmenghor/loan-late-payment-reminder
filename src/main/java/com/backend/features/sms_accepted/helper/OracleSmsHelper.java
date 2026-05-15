package com.backend.features.sms_accepted.helper;

import com.backend.features.notification.helper.OracleDwhConnection;
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

    private final OracleDwhConnection oracleDwhConnection;

    public List<OracleSmsDto> selectProcessingSmsRecords() {
        List<OracleSmsDto> records = new ArrayList<>();
        String query = "SELECT MSG_ID, TELL, DESCRIPTION FROM D_CBS_SMS_LOG WHERE SMS_STATUS = 'PROCESSING'";

        try (Connection con = oracleDwhConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                OracleSmsDto record = OracleSmsDto.builder()
                        .msgId(rs.getString("MSG_ID"))
                        .phone(rs.getString("TELL"))
                        .message(rs.getString("DESCRIPTION"))
                        .build();
                records.add(record);
            }

            log.info("Oracle DWH: Selected {} PROCESSING SMS records from D_CBS_SMS_LOG", records.size());
            return records;

        } catch (SQLException e) {
            log.error("Oracle DWH: Error fetching SMS records from D_CBS_SMS_LOG | Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to select SMS records from Oracle DWH", e);
        }
    }

    public void updateSmsStatus(String msgId, String status) {
        String query = "UPDATE D_CBS_SMS_LOG SET SMS_STATUS = ? WHERE MSG_ID = ?";

        try (Connection con = oracleDwhConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, status);
            ps.setString(2, msgId);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                log.info("Oracle DWH: Updated SMS_STATUS to '{}' for MSG_ID: {}", status, msgId);
            } else {
                log.warn("Oracle DWH: No records updated for MSG_ID: {}", msgId);
            }

        } catch (SQLException e) {
            log.error("Oracle DWH: Error updating SMS_STATUS | MSG_ID: {}, Error: {}", msgId, e.getMessage(), e);
            throw new RuntimeException("Failed to update SMS_STATUS in Oracle DWH", e);
        }
    }
}
