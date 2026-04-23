package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmsLogRepository extends BaseRepository<SmsLog, UUID> {
    Optional<SmsLog> findByCustomerIdAndPhoneNumberAndReportDateAndStatus(
            String customerId, String phoneNumber, LocalDate reportDate, String status);
}
