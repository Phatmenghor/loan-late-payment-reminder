package com.backend.features.sms.repository;

import com.backend.features.sms.models.SmsLog;
import com.backend.shared.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsRepository extends BaseRepository<SmsLog, String> {

    Optional<SmsLog> findByMsgId(String msgId);

    @Query("SELECT s FROM SmsLog s WHERE s.msgId = :msgId AND s.phone = :phone")
    Optional<SmsLog> findByMsgIdAndPhone(@Param("msgId") String msgId, @Param("phone") String phone);

    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.smsStatus = 'SUCCESS'")
    long countSuccess();

    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.smsStatus = 'ERROR'")
    long countError();
}
