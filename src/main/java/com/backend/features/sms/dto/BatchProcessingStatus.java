package com.backend.features.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchProcessingStatus {

    @JsonProperty("total_records")
    private int totalRecords;

    @JsonProperty("processed_count")
    private int processedCount;

    @JsonProperty("success_count")
    private int successCount;

    @JsonProperty("failure_count")
    private int failureCount;

    @JsonProperty("progress_percentage")
    private double progressPercentage;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("started_at")
    private LocalDateTime startedAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
