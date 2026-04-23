package com.backend.features.notification.models;

import com.backend.shared.domain.BaseUUIDEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "loan_sms_log",
    indexes = {
        @Index(name = "idx_phone_number", columnList = "phone_number"),
        @Index(name = "idx_sms_status", columnList = "sms_status"),
        @Index(name = "idx_sms_log_date", columnList = "sms_log_date"),
        @Index(name = "idx_is_deleted", columnList = "is_deleted")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SmsLog extends BaseUUIDEntity {

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "sms_status", length = 100)
    private String smsStatus;

    @Column(name = "sms_log_date")
    private LocalDateTime smsLogDate;

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;

    @Override
    public String toString() {
        return String.format(
            "SmsLog(id=%s, phoneNumber=%s, smsStatus=%s, smsLogDate=%s)",
            getId(), phoneNumber, smsStatus, smsLogDate
        );
    }
}
