package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_pending_queue", indexes = {
        @Index(name = "idx_queue_status", columnList = "status"),
        @Index(name = "idx_queue_report_date", columnList = "report_date"),
        @Index(name = "idx_queue_phone", columnList = "phone_number"),
        @Index(name = "idx_queue_status_date", columnList = "status, report_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SmsPendingQueue extends BaseUUIDEntity {

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "arrangement_id")
    private String arrangementId;

    @Column(name = "day_due")
    private Integer dayDue;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "message_content")
    private String messageContent;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, SUCCESS, FAILURE

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
}
