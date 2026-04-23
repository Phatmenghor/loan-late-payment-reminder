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
             PreparedStatement ps = con.prepareStatement("SELECT SET_DESC FROM D_CBS_SETTING")) {

            ResultSet rs = ps.executeQuery();
            String description = "";

            if (rs.next()) {
                description = rs.getString("SET_DESC");
                log.debug("SMS message content fetched from Oracle: {}", description);
            }

            return description.isEmpty() ? "Loan payment reminder" : description;

        } catch (SQLException e) {
            log.error("Error fetching message content from D_CBS_SETTING: {}", e.getMessage(), e);
            return "Loan payment reminder";
        }
    }
}
