package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsPendingQueue;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmsPendingQueueRepository extends BaseRepository<SmsPendingQueue, UUID> {

    @Query("SELECT q FROM SmsPendingQueue q WHERE q.status = :status AND q.reportDate = :reportDate")
    List<SmsPendingQueue> findByStatusAndReportDate(@Param("status") String status, @Param("reportDate") LocalDate reportDate);

    @Query("SELECT COUNT(q) FROM SmsPendingQueue q WHERE q.status = :status AND q.reportDate = :reportDate")
    long countByStatusAndReportDate(@Param("status") String status, @Param("reportDate") LocalDate reportDate);

    @Query("SELECT q FROM SmsPendingQueue q WHERE q.reportDate = :reportDate")
    List<SmsPendingQueue> findByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT q FROM SmsPendingQueue q WHERE q.customerId = :customerId AND q.phoneNumber = :phoneNumber AND q.reportDate = :reportDate AND q.status = 'SUCCESS'")
    Optional<SmsPendingQueue> findByCustomerIdAndPhoneAndDateAndSuccess(
            @Param("customerId") String customerId,
            @Param("phoneNumber") String phoneNumber,
            @Param("reportDate") LocalDate reportDate);

    @Query("SELECT COUNT(q) FROM SmsPendingQueue q WHERE q.reportDate = :reportDate AND q.status = 'PENDING'")
    long countPendingByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT COUNT(q) FROM SmsPendingQueue q WHERE q.reportDate = :reportDate AND q.status = 'FAILURE'")
    long countFailureByReportDate(@Param("reportDate") LocalDate reportDate);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sms_pending_queue
            SET status = :status,
                processed_at = :processedAt,
                failure_reason = CASE WHEN :failureReason IS NOT NULL THEN :failureReason ELSE failure_reason END,
                retry_count = CASE WHEN :status = 'FAILURE' THEN retry_count + 1 ELSE retry_count END
            WHERE id = :id
            """, nativeQuery = true)
    int updateQueueStatusById(
            @Param("id") UUID id,
            @Param("status") String status,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("failureReason") String failureReason);
}
