package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsFailureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmsFailureLogRepository extends JpaRepository<SmsFailureLog, UUID> {

    Optional<SmsFailureLog> findByPhoneNumberAndReportDate(String phoneNumber, LocalDate reportDate);

    List<SmsFailureLog> findByStatusAndReportDate(String status, LocalDate reportDate);

    @Query("SELECT s FROM SmsFailureLog s WHERE s.reportDate = :reportDate AND s.status != 'SVC-SUCCESS-00'")
    List<SmsFailureLog> findFailedRecordsByReportDate(@Param("reportDate") LocalDate reportDate);

    @Query("SELECT s FROM SmsFailureLog s WHERE s.phoneNumber = :phoneNumber AND s.reportDate = :reportDate")
    Optional<SmsFailureLog> findByPhoneAndDate(@Param("phoneNumber") String phoneNumber, @Param("reportDate") LocalDate reportDate);
}
