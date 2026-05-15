package com.backend.features.sms_accepted.repository;

import com.backend.features.sms_accepted.models.SmsAcceptedLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsAcceptedRepository extends BaseRepository<SmsAcceptedLog, String> {

    Optional<SmsAcceptedLog> findByMsgId(String msgId);

    @Query("SELECT s FROM SmsAcceptedLog s WHERE s.msgId = :msgId AND s.phone = :phone")
    Optional<SmsAcceptedLog> findByMsgIdAndPhone(@Param("msgId") String msgId, @Param("phone") String phone);

    @Query("SELECT COUNT(s) FROM SmsAcceptedLog s WHERE s.smsStatus = 'PROCESSING'")
    long countProcessing();

    @Query("SELECT COUNT(s) FROM SmsAcceptedLog s WHERE s.smsStatus = 'SUCCESS'")
    long countSuccess();

    @Query("SELECT COUNT(s) FROM SmsAcceptedLog s WHERE s.smsStatus = 'FAILURE'")
    long countFailure();
}
