package com.backend.features.notification.service.impl;

import com.backend.config.CpbApiConfig;
import com.backend.features.notification.dto.ReceptionFormatDto;
import com.backend.features.notification.helper.NotificationLogger;
import com.backend.features.notification.helper.NotificationPayloadBuilder;
import com.backend.features.notification.helper.OracleHelper;
import com.backend.features.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String SMS_STATUS_SUCCESS = "SVC-SUCCESS-00";
    private static final String SMS_STATUS_FAILED = "SVC-FAILED";

    private final RestTemplate restTemplate;
    private final NotificationPayloadBuilder payloadBuilder;
    private final CpbApiConfig cpbApiConfig;
    private final NotificationLogger notificationLogger;
    private final OracleHelper oracleHelper;

    @Override
    public void processPendingSmsNotifications() {
        notificationLogger.logProcessStart();

        try {
            // Step 1: SELECT all pending SMS phone numbers from Oracle
            List<String> phoneNumbers = oracleHelper.selectPendingSmsPhoneNumbers();

            if (phoneNumbers.isEmpty()) {
                notificationLogger.logNoPendingSms();
                notificationLogger.logProcessEnd(0, 0);
                return;
            }

            notificationLogger.logPendingSmsFetched(phoneNumbers.size());

            // Step 2: Loop through each pending SMS
            int successCount = 0;
            int failureCount = 0;

            for (String phoneNumber : phoneNumbers) {
                try {
                    notificationLogger.logSmsProcessStart(phoneNumber);

                    // Step 1: Send SMS to API
                    String apiResponse = sendSmsToApi(phoneNumber, "Loan payment reminder");

                    // Step 2: UPDATE SMS status in Oracle
                    oracleHelper.updateSmsStatus(phoneNumber, apiResponse);
                    successCount++;
                    notificationLogger.logSmsSuccess(phoneNumber, apiResponse);
                    notificationLogger.logSmsProcessEnd(phoneNumber);

                } catch (Exception e) {
                    failureCount++;
                    notificationLogger.logSmsFailure(phoneNumber, e.getMessage());
                    try {
                        oracleHelper.updateSmsStatus(phoneNumber, SMS_STATUS_FAILED);
                    } catch (Exception ex) {
                        notificationLogger.logException("Oracle UPDATE on failure", ex);
                    }
                    notificationLogger.logException("SMS Processing", e);
                }
            }

            notificationLogger.logProcessEnd(successCount, failureCount);

        } catch (Exception e) {
            notificationLogger.logException("Pending SMS Processing", e);
        }
    }


    // ===== PRIVATE METHODS =====

    private String sendSmsToApi(String phoneNumber, String messageContent) throws RestClientException {
        try {
            String jsonPayload = payloadBuilder.buildJsonPayload(phoneNumber, messageContent);
            log.debug("API: Preparing SMS payload for phone: {}", phoneNumber);

            String apiUrl = cpbApiConfig.getUrl() + "/SendOTT";
            log.info("API: Sending request to: {}", apiUrl);

            ResponseEntity<ReceptionFormatDto> apiResponse = restTemplate.postForEntity(
                    apiUrl,
                    jsonPayload,
                    ReceptionFormatDto.class
            );

            if (apiResponse.getBody() != null && apiResponse.getBody().getDesc() != null) {
                String responseStatus = apiResponse.getBody().getDesc();
                log.info("API: Received response status: {}", responseStatus);
                return responseStatus;
            }

            log.warn("API: No response body received");
            return SMS_STATUS_FAILED;

        } catch (RestClientException e) {
            log.error("API: Request failed for phone: {}", phoneNumber, e);
            throw e;
        }
    }
}
