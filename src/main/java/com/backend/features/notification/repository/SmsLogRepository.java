package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SmsLogRepository extends BaseRepository<SmsLog, UUID> {

    @Query("SELECT s FROM SmsLog s WHERE s.smsStatus != :status AND s.isDeleted = false")
    List<SmsLog> findAllByStatusNotEqual(@Param("status") String status);

    @Query("SELECT s FROM SmsLog s WHERE s.phoneNumber = :phoneNumber AND s.isDeleted = false")
    List<SmsLog> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT s FROM SmsLog s WHERE s.smsLogDate >= :startDate AND s.smsLogDate <= :endDate AND s.isDeleted = false")
    List<SmsLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
