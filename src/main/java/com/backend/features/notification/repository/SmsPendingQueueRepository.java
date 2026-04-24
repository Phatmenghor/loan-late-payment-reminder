package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsPendingQueue;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
