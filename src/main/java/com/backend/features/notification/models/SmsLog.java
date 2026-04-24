package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_sms_log", indexes = {
        @Index(name = "idx_sms_log_phone_number", columnList = "phone_number"),
        @Index(name = "idx_sms_log_customer_id", columnList = "customer_id"),
        @Index(name = "idx_sms_log_status", columnList = "sms_status"),
        @Index(name = "idx_sms_log_report_date", columnList = "report_date"),
        @Index(name = "idx_sms_log_date", columnList = "sms_log_date"),
        @Index(name = "idx_sms_log_is_deleted", columnList = "is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SmsLog extends BaseUUIDEntity {

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "arrangement_id")
    private String arrangementId;

    @Column(name = "day_due")
    private Integer dayDue;

    @Column(name = "message_content")
    private String messageContent;

    @Column(name = "sms_status", nullable = false)
    private String smsStatus;

    @Column(name = "sms_log_date")
    private LocalDateTime smsLogDate;
}
