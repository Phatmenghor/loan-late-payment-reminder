package com.backend.features.notification.helper;

import com.backend.features.notification.helper.OracleConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@Slf4j
public class CpbHelper {

    public String getContentDescription() {
        try (Connection con = OracleConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT SET_DESC FROM APPS.D_CBS_SETTING WHERE SET_CODE='SMS_LOAN_LATE'")) {

            ResultSet rs = ps.executeQuery();
            String description = "";

            if (rs.next()) {
                description = rs.getString("SET_DESC");
                log.info("SMS message content fetched from Oracle APPS.D_CBS_SETTING: {}", description);
            }

            return description.isEmpty() ? "Loan payment reminder" : description;

        } catch (SQLException e) {
            log.warn("Could not fetch from APPS.D_CBS_SETTING ({}), using default message. Ensure APPS.D_CBS_SETTING table exists with SET_CODE='SMS_LOAN_LATE' row.", e.getMessage());
            return "Loan payment reminder";
        }
    }
}
