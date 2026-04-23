package com.backend.features.src.main.java.com.cpbank.helper;

import com.cpbank.database.OracleDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CpbHelper {

    private static final Logger logger = LoggerFactory.getLogger(CpbHelper.class);

    public String getContentDescription() {
        try (Connection con = OracleDatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT * FROM D_CBS_SETTING")) {
            ResultSet rs = ps.executeQuery();
            String Description = "";
            while (rs.next()) {
                Description = rs.getString("SET_DESC");
            }
            return Description;

        } catch (SQLException e) {
            logger.error("Error fetching phone numbers from the database: {}", e.getMessage());
        }
        return "";
    }

}