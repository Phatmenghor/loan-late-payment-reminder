package com.backend.features.notification.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_failure_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsFailureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

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

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_date")
    private LocalDateTime lastRetryDate;

    @Column(name = "status")
    private String status;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
