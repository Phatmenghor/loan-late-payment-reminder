package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmsLogRepository extends BaseRepository<SmsLog, UUID> {
    Optional<SmsLog> findByCustomerIdAndPhoneNumberAndStatus(String customerId, String phoneNumber, String status);
}
