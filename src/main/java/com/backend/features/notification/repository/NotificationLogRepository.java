package com.backend.features.notification.repository;

import com.backend.features.notification.models.NotificationLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends BaseRepository<NotificationLog, UUID> {
    Optional<NotificationLog> findByCustomerIdAndPhoneNumberAndReportDateAndSmsStatus(
            String customerId, String phoneNumber, LocalDate reportDate, String smsStatus);
}
