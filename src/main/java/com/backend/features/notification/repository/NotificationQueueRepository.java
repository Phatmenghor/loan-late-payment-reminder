package com.backend.features.notification.repository;

import com.backend.features.notification.models.NotificationQueue;
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
public interface NotificationQueueRepository extends BaseRepository<NotificationQueue, UUID> {

    @Query("SELECT q FROM NotificationQueue q WHERE q.status = :status AND q.reportDate = :reportDate AND q.isDeleted = false")
    List<NotificationQueue> findByStatusAndReportDate(@Param("status") String status, @Param("reportDate") LocalDate reportDate);

    @Query("SELECT COUNT(q) FROM NotificationQueue q WHERE q.status = :status AND q.reportDate = :reportDate AND q.isDeleted = false")
    long countByStatusAndReportDate(@Param("status") String status, @Param("reportDate") LocalDate reportDate);

    @Query("SELECT q FROM NotificationQueue q WHERE q.reportDate = :reportDate AND q.isDeleted = false")
    List<NotificationQueue> findByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT q FROM NotificationQueue q WHERE q.reportDate = :reportDate AND (q.status = 'PENDING' OR q.status = 'FAILURE') AND q.isDeleted = false")
    List<NotificationQueue> findPendingAndFailureByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT q FROM NotificationQueue q WHERE q.customerId = :customerId AND q.phoneNumber = :phoneNumber AND q.reportDate = :reportDate AND q.status = 'SUCCESS' AND q.isDeleted = false")
    Optional<NotificationQueue> findByCustomerIdAndPhoneAndDateAndSuccess(
            @Param("customerId") String customerId,
            @Param("phoneNumber") String phoneNumber,
            @Param("reportDate") LocalDate reportDate);

    @Query("SELECT COUNT(q) FROM NotificationQueue q WHERE q.reportDate = :reportDate AND q.status = 'PENDING' AND q.isDeleted = false")
    long countPendingByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT COUNT(q) FROM NotificationQueue q WHERE q.reportDate = :reportDate AND q.status = 'FAILURE' AND q.isDeleted = false")
    long countFailureByReportDate(@Param("reportDate") LocalDate reportDate);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE notification_queue
            SET status = :status,
                processed_at = :processedAt,
                failure_reason = CASE WHEN :failureReason IS NOT NULL THEN :failureReason ELSE failure_reason END,
                retry_count = CASE WHEN :status = 'FAILURE' THEN retry_count + 1 ELSE retry_count END,
                updated_at = :processedAt
            WHERE id = :id AND is_deleted = false
            """, nativeQuery = true)
    int updateQueueStatusById(
            @Param("id") UUID id,
            @Param("status") String status,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("failureReason") String failureReason);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM notification_queue
            WHERE status = 'SUCCESS' AND created_at < :twoDaysAgo AND is_deleted = false
            """, nativeQuery = true)
    int deleteSuccessfulRecordsOlderThan(@Param("twoDaysAgo") LocalDateTime twoDaysAgo);
}
