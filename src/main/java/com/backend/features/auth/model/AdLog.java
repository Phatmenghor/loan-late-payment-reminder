package com.backend.features.auth.model;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_log")
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
