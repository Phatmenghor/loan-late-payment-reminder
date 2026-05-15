package com.backend.features.sms_accepted.service.impl;

import com.backend.config.MobileBankingConfig;
import com.backend.features.sms_accepted.dto.OracleSmsDto;
import com.backend.features.sms_accepted.dto.SendAcceptedSmsRequest;
import com.backend.features.sms_accepted.dto.SendAcceptedSmsResponse;
import com.backend.features.sms_accepted.helper.OracleSmsHelper;
import com.backend.features.sms_accepted.mapper.SmsAcceptedMapper;
import com.backend.features.sms_accepted.models.SmsAcceptedLog;
import com.backend.features.sms_accepted.repository.SmsAcceptedRepository;
import com.backend.features.sms_accepted.service.SmsAcceptedService;
import com.backend.shared.utils.HttpClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsAcceptedServiceImpl implements SmsAcceptedService {

    private final SmsAcceptedRepository smsAcceptedRepository;
    private final SmsAcceptedMapper smsAcceptedMapper;
    private final MobileBankingConfig mobileBankingConfig;
    private final HttpClientUtil httpClientUtil;
    private final OracleSmsHelper oracleSmsHelper;

    @Override
    @Transactional
    public SendAcceptedSmsResponse sendSms(SendAcceptedSmsRequest request) {
        String msgId = request.getMsgId() != null ? request.getMsgId() : String.valueOf(System.currentTimeMillis());
        String phone = request.getPhone();
        String message = request.getMessage();

        log.info("Processing SMS send request - msgId: {}, phone: {}", msgId, phone);

        SmsAcceptedLog smsLog = SmsAcceptedLog.builder()
                .msgId(msgId)
                .phone(phone)
                .smsContent(message)
                .smsStatus("PROCESSING")
                .createdAt(LocalDateTime.now())
                .build();

        smsLog = smsAcceptedRepository.save(smsLog);
        log.info("SMS log created - ID: {}, msgId: {}", smsLog.getId(), msgId);

        try {
            sendSoapSms(phone, message, msgId, smsLog);
        } catch (Exception e) {
            log.error("Error sending SMS via SOAP - phone: {}, msgId: {}", phone, msgId, e);
            updateSmsStatus(smsLog.getId().toString(), "FAILURE", null, e.getMessage());
        }

        return smsAcceptedMapper.toSendAcceptedSmsResponse(smsLog);
    }

    @Override
    @Transactional
    public void processPendingSmsFromOracle() {
        log.info("Starting processing pending SMS from Oracle VIEW_SMS");

        try {
            List<OracleSmsDto> pendingRecords = oracleSmsHelper.selectSmsPendingRecords();
            log.info("Found {} pending SMS records from Oracle", pendingRecords.size());

            for (OracleSmsDto record : pendingRecords) {
                SendAcceptedSmsRequest request = SendAcceptedSmsRequest.builder()
                        .msgId(record.getMsgId())
                        .phone(record.getPhone())
                        .message(record.getMessage())
                        .build();

                sendSms(request);
            }

            log.info("Completed processing {} SMS records from Oracle", pendingRecords.size());
        } catch (Exception e) {
            log.error("Error processing pending SMS from Oracle", e);
            throw new RuntimeException("Failed to process pending SMS from Oracle", e);
        }
    }

    @Override
    public SendAcceptedSmsResponse getStatus(String msgId) {
        log.info("Fetching SMS status for msgId: {}", msgId);

        return smsAcceptedRepository.findByMsgId(msgId)
                .map(smsAcceptedMapper::toSendAcceptedSmsResponse)
                .orElseThrow(() -> new RuntimeException("SMS record not found for msgId: " + msgId));
    }

    private void sendSoapSms(String phone, String message, String msgId, SmsAcceptedLog smsRecord) {
        String otpUrl = mobileBankingConfig.getOtpUrl();
        String secretKey = mobileBankingConfig.getSecretKey();
        String requestID = msgId;

        String soapXml = buildSoapRequest(requestID, phone, message, secretKey);

        try {
            log.info("Sending SOAP SMS request - msgId: {}, phone: {}", msgId, phone);

            String responseXml = httpClientUtil.postForString(otpUrl, soapXml, "application/soap+xml");

            Matcher matcher = Pattern.compile("<(?:\\w+:)?return>(.*?)</(?:\\w+:)?return>").matcher(responseXml);
            String jsonPayload = matcher.find() ? matcher.group(1) : null;

            log.info("SOAP SMS response received - msgId: {}", msgId);
            if (jsonPayload != null) {
                log.info("SOAP SMS payload - msgId: {}, payload: {}", msgId, jsonPayload);
            }

            if (jsonPayload != null && jsonPayload.contains("\"rescode\":\"00\"")) {
                log.info("SMS sent successfully - msgId: {}, phone: {}", msgId, phone);
                updateSmsStatus(smsRecord.getId().toString(), "SUCCESS", "00", "SMS sent successfully");
                oracleSmsHelper.updateSmsStatus(msgId, "SUCCESS");
            } else {
                log.warn("SMS sending may have failed - msgId: {}, response: {}", msgId, jsonPayload);
                updateSmsStatus(smsRecord.getId().toString(), "FAILURE", null, "SOAP response: " + jsonPayload);
                oracleSmsHelper.updateSmsStatus(msgId, "FAILURE");
            }

        } catch (Exception e) {
            log.error("Failed to send SOAP SMS - msgId: {}, phone: {}", msgId, phone, e);
            updateSmsStatus(smsRecord.getId().toString(), "FAILURE", null, e.getMessage());
            oracleSmsHelper.updateSmsStatus(msgId, "FAILURE");
        }
    }

    private String buildSoapRequest(String requestID, String phone, String message, String secretKey) {
        return "<?xml version=\"1.0\"?>"
                + "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' "
                + "xmlns:cpb='http://cpbmobile.vnpay.vn'>"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<cpb:sendSmsNew>"
                + "<cpb:requestId>" + requestID + "</cpb:requestId>"
                + "<cpb:keyword>CPBSMS</cpb:keyword>"
                + "<cpb:mobileNo>" + phone + "</cpb:mobileNo>"
                + "<cpb:content>" + message + "</cpb:content>"
                + "<cpb:requestTime></cpb:requestTime>"
                + "<cpb:contentType>9</cpb:contentType>"
                + "<cpb:secretKey>" + secretKey + "</cpb:secretKey>"
                + "</cpb:sendSmsNew>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    @Transactional
    private void updateSmsStatus(String logId, String status, String responseCode, String message) {
        smsAcceptedRepository.findById(logId).ifPresent(smsLog -> {
            smsLog.setSmsStatus(status);
            smsLog.setResponseCode(responseCode);
            smsLog.setResponseMessage(message);
            smsLog.setSentAt(LocalDateTime.now());
            smsAcceptedRepository.save(smsLog);
            log.info("Updated SMS status - logId: {}, status: {}", logId, status);
        });
    }
}
