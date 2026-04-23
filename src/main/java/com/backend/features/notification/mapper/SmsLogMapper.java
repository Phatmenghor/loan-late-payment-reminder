package com.backend.features.notification.mapper;

import com.backend.features.notification.dto.response.SmsLogResponse;
import com.backend.features.notification.models.SmsLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SmsLogMapper {

    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "smsStatus", target = "smsStatus")
    @Mapping(source = "smsLogDate", target = "smsLogDate")
    @Mapping(source = "messageContent", target = "messageContent")
    @Mapping(source = "createdAt", target = "createdAt")
    SmsLogResponse toResponse(SmsLog smsLog);
}
