package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_sms_log", indexes = {
        @Index(name = "idx_phone_number", columnList = "phone_number"),
        @Index(name = "idx_sms_status", columnList = "sms_status"),
        @Index(name = "idx_sms_log_date", columnList = "sms_log_date"),
        @Index(name = "idx_is_deleted", columnList = "is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SmsLog extends BaseUUIDEntity {

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "message_content")
    private String messageContent;

    @Column(name = "sms_status", nullable = false)
    private String smsStatus;

    @Column(name = "sms_log_date")
    private LocalDateTime smsLogDate;
}
