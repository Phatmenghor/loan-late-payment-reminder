package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_processing_status", indexes = {
        @Index(name = "idx_report_date", columnList = "report_date"),
        @Index(name = "idx_is_complete", columnList = "is_complete")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class ProcessingStatus extends BaseUUIDEntity {

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_customers", nullable = false)
    private Integer totalCustomers;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column(name = "is_complete", nullable = false)
    private Boolean isComplete;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
