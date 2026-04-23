package com.backend.features.src.main.java.com.cpbank.service.impl;

import com.cpbank.database.OracleDatabaseConnection;
import com.cpbank.helper.CpbHelper;
import com.cpbank.helper.JsonHelper;
import com.cpbank.model.ReceptionFormat;
import com.cpbank.service.SendNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SendNotificationServiceImpl implements SendNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SendNotificationServiceImpl.class);
    private final RestTemplate restTemplate;
    private final JsonHelper jsonHelper;
    private final CpbHelper cpbHelper;

    @Value("${apiUrl}")
    private String apiUrl;

    @Override
    public void pushNotificationsToMobile() {
        try (Connection con = OracleDatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT Tell FROM D_Cbs_Sms_Log_Test WHERE Sms_Status != ?")) {
            ps.setString(1, "SVC-SUCCESS-00");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String phone = rs.getString("Tell");
                sendNotification(phone);
            }

        } catch (SQLException e) {
            logger.error("Error fetching phone numbers from the database: {}", e.getMessage());
        }
    }


    private void sendNotification(String phone) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String content = cpbHelper.getContentDescription();
        try {
            String jsonPayload = jsonHelper.createJsonPayload(phone, content);
            logger.info("Payload: {}", jsonPayload);

            ResponseEntity<ReceptionFormat> response = restTemplate.postForEntity(apiUrl + "/SendOTT", jsonPayload, ReceptionFormat.class);
            logger.info("Response : {}", response.getBody());

            logger.info("Notification is sent to phone number: {}", phone);

            // Update Status
            String query = "UPDATE D_Cbs_Sms_Log_Test SET Sms_Status = ? , Sms_Log_Dt = ? WHERE Tell = ?";
            try (Connection con = OracleDatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, response.getBody().getDesc());
                ps.setTimestamp(2, Timestamp.from(Instant.now()));
                ps.setString(3, phone);

                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    logger.info("Data updated successfully!");
                }
            } catch (SQLException e) {
                logger.error("SQL Error: {}", e.getMessage());
                throw new SQLException("Error updating data", e);
            }

        } catch (Exception e) {
            logger.error("Error sending notification to phone {}: {}", phone, e.getMessage());
        }
    }



}