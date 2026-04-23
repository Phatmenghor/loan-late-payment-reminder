package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_failure_log", indexes = {
        @Index(name = "idx_phone_number", columnList = "phone_number"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_report_date", columnList = "report_date"),
        @Index(name = "idx_is_deleted", columnList = "is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SmsFailureLog extends BaseUUIDEntity {

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "arrangement_id")
    private String arrangementId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_retry_date")
    private LocalDateTime lastRetryDate;

    @Column(name = "status")
    private String status;
}
