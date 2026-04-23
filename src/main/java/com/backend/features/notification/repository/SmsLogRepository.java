package com.backend.features.notification.repository;

import com.backend.features.notification.models.SmsLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsLogRepository extends BaseRepository<SmsLog, Long> {

    @Query("SELECT s FROM SmsLog s WHERE s.smsStatus != :status")
    List<SmsLog> findAllByStatusNotEqual(@Param("status") String status);

    List<SmsLog> findByPhoneNumber(String phoneNumber);
}
