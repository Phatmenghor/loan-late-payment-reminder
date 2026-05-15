package com.backend.features.sms.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_log", indexes = {
        @Index(name = "idx_sms_log_msg_id", columnList = "msg_id"),
        @Index(name = "idx_sms_log_phone", columnList = "phone"),
        @Index(name = "idx_sms_log_status", columnList = "sms_status"),
        @Index(name = "idx_sms_log_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SmsLog extends BaseUUIDEntity {

    @Column(name = "msg_id", nullable = false, unique = true)
    private String msgId;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "sms_content", columnDefinition = "TEXT")
    private String smsContent;

    @Column(name = "sms_status", nullable = false)
    private String smsStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}
