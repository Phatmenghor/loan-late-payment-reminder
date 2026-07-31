package com.backend.features.notification.helper;

import com.backend.features.notification.dto.LoanLateReminderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class OracleHelper {

    private final OracleConnection oracleConnection;

    public List<LoanLateReminderDto> selectLoanLateReminderRecords() {
        List<LoanLateReminderDto> records = new ArrayList<>();
        String query = "SELECT * FROM VIEW_LOAN_LATE_REMINDER";

        try (Connection con = oracleConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String reportDateStr = rs.getString("REPORTDATE");
                LocalDate reportDate = parseOracleDate(reportDateStr);

                LoanLateReminderDto record = LoanLateReminderDto.builder()
                        .reportDate(reportDate)
                        .customerId(rs.getString("CUSTOMERID"))
                        .phoneNumber(rs.getString("MBAPP_PHONE"))
                        .arrangementId(rs.getString("ARRANGEMENT"))
                        .build();
                records.add(record);
            }

            log.info("Oracle: Selected {} notification records from view", records.size());
            return records;

        } catch (SQLException e) {
            log.error("Oracle: Error fetching notification records from view | Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to select notification records from Oracle", e);
        }
    }

    private LocalDate parseOracleDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ENGLISH);
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            try {
                String[] parts = dateStr.split("-");
                if (parts.length == 3) {
                    int day = Integer.parseInt(parts[0]);
                    String monthStr = parts[1].toUpperCase();
                    int year = Integer.parseInt(parts[2]);
                    if (year < 100) year += 2000;

                    int month = switch (monthStr) {
                        case "JAN" -> 1;
                        case "FEB" -> 2;
                        case "MAR" -> 3;
                        case "APR" -> 4;
                        case "MAY" -> 5;
                        case "JUN" -> 6;
                        case "JUL" -> 7;
                        case "AUG" -> 8;
                        case "SEP" -> 9;
                        case "OCT" -> 10;
                        case "NOV" -> 11;
                        case "DEC" -> 12;
                        default -> throw new IllegalArgumentException("Unknown month: " + monthStr);
                    };

                    return LocalDate.of(year, month, day);
                }
            } catch (Exception ex) {
                log.error("Failed to parse date: {}", dateStr, ex);
            }
            throw new RuntimeException("Could not parse date: " + dateStr);
        }
    }
}


