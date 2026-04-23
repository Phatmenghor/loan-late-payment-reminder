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

    public List<LoanLateReminderDto> selectLoanLateReminderRecords(LocalDate reportDate) {
        List<LoanLateReminderDto> records = new ArrayList<>();
        String query = "SELECT REPORTDATE, CUSTOMERID, MBAPP_PHONE, ARRANGEMENT_ID, DAYDUE FROM STG.VIEW_LOAN_LATE_REMINDER WHERE REPORTDATE = ?";

        try (Connection con = OracleConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LoanLateReminderDto record = LoanLateReminderDto.builder()
                        .reportDate(rs.getDate("REPORTDATE").toLocalDate())
                        .customerId(rs.getString("CUSTOMERID"))
                        .phoneNumber(rs.getString("MBAPP_PHONE"))
                        .arrangementId(rs.getString("ARRANGEMENT_ID"))
                        .dayDue(rs.getInt("DAYDUE"))
                        .build();
                records.add(record);
            }

            log.info("Oracle: Selected {} loan late reminder records for date: {}", records.size(), reportDate);
            return records;

        } catch (SQLException e) {
            log.error("Oracle: Error fetching loan late reminder records for date: {} | Error: {}", reportDate, e.getMessage(), e);
            throw new RuntimeException("Failed to select loan late reminder records from Oracle", e);
        }
    }
}


