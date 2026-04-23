package com.backend.features.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanLateReminderDto {

    private LocalDate reportDate;
    private String customerId;
    private String phoneNumber;
    private String arrangementId;
    private Integer dayDue;
}
