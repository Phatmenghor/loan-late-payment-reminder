package com.backend.features.notification.repository;

import com.backend.features.notification.models.NotificationProcessingStatus;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationProcessingStatusRepository extends BaseRepository<NotificationProcessingStatus, UUID> {

    @Query("SELECT p FROM NotificationProcessingStatus p WHERE p.reportDate = :reportDate")
    Optional<NotificationProcessingStatus> findByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT p FROM NotificationProcessingStatus p WHERE p.reportDate = :reportDate AND p.isComplete = true")
    Optional<NotificationProcessingStatus> findCompleteByReportDate(@Param("reportDate") LocalDate reportDate);
}
