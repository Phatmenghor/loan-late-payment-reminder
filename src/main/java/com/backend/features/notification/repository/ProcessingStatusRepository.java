package com.backend.features.notification.repository;

import com.backend.features.notification.models.ProcessingStatus;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessingStatusRepository extends BaseRepository<ProcessingStatus, UUID> {

    @Query("SELECT p FROM ProcessingStatus p WHERE p.reportDate = :reportDate")
    Optional<ProcessingStatus> findByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT p FROM ProcessingStatus p WHERE p.reportDate = :reportDate AND p.isComplete = true")
    Optional<ProcessingStatus> findCompleteByReportDate(@Param("reportDate") LocalDate reportDate);
}
