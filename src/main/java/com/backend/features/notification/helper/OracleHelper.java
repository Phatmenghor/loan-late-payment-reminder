package com.backend.features.notification.helper;

import com.backend.features.notification.dto.LoanLateReminderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OracleHelper {

    public List<LoanLateReminderDto> selectLoanLateReminderRecords() {
        List<LoanLateReminderDto> records = new ArrayList<>();
        String query = "SELECT * FROM VIEW_LOAN_LATE_REMINDER";

        try (Connection con = OracleConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LoanLateReminderDto record = LoanLateReminderDto.builder()
                        .reportDate(rs.getDate("REPORTDATE").toLocalDate())
                        .customerId(rs.getString("CUSTOMERID"))
                        .phoneNumber(rs.getString("MBAPP_PHONE"))
                        .arrangementId(rs.getString("ARRANGEMENT"))
                        .dayDue(null)
                        .build();
                records.add(record);
            }

            log.info("Oracle: Selected {} loan late reminder records from view", records.size());
            return records;

        } catch (SQLException e) {
            log.error("Oracle: Error fetching loan late reminder records from view | Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to select loan late reminder records from Oracle", e);
        }
    }
}


