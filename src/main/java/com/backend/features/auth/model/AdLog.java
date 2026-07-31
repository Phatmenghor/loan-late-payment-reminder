package com.backend.features.auth.model;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ad_log",
    indexes = {
        @Index(name = "idx_ad_log_trace_id", columnList = "trace_id"),
        @Index(name = "idx_ad_log_client_ip", columnList = "client_ip"),
        @Index(name = "idx_ad_log_app_name", columnList = "app_name"),
        @Index(name = "idx_ad_log_api_key", columnList = "api_key"),
        @Index(name = "idx_ad_log_username", columnList = "username"),
        @Index(name = "idx_ad_log_called_at", columnList = "called_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AdLog extends BaseUUIDEntity {

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "client_ip", length = 100)
    private String clientIp;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "ad_attributes", columnDefinition = "TEXT")
    private String adAttributes;

    @Column(name = "called_at")
    private LocalDateTime calledAt;
}
