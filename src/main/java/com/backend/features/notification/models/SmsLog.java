package com.backend.features.notification.models;

import com.backend.shared.models.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "D_CBS_SMS_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "sms_status")
    private String smsStatus;

    @Column(name = "sms_log_date")
    private LocalDateTime smsLogDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
