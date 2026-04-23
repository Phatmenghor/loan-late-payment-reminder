package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_sms_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SmsLog extends BaseUUIDEntity {

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "sms_status", length = 100)
    private String smsStatus;

    @Column(name = "sms_log_date")
    private LocalDateTime smsLogDate;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;
}
