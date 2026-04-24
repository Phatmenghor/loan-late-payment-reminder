package com.backend.features.notification.helper;

import com.backend.features.notification.dto.LoanLateReminderDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class TestDataHelper {

    public List<LoanLateReminderDto> getTestLoanReminders() {
        List<LoanLateReminderDto> testRecords = new ArrayList<>();

        testRecords.add(LoanLateReminderDto.builder()
                .reportDate(LocalDate.now().minusDays(1))
                .customerId("CUST001")
                .phoneNumber("070411260")
                .arrangementId("ARR001")
                .dayDue(null)
                .build());

        testRecords.add(LoanLateReminderDto.builder()
                .reportDate(LocalDate.now().minusDays(1))
                .customerId("CUST002")
                .phoneNumber("070411263424sdf0")
                .arrangementId("ARR002")
                .dayDue(null)
                .build());

        return testRecords;
    }
}
