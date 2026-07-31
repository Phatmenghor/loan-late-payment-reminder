package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationLog extends BaseUUIDEntity {

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "arrangement_id")
    private String arrangementId;

    @Column(name = "json_payload", columnDefinition = "TEXT")
    private String jsonPayload;

    @Column(name = "notification_status", nullable = false)
    private String notificationStatus;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "notification_log_date")
    private LocalDateTime notificationLogDate;
}
