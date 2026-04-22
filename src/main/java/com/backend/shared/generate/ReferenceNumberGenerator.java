package com.backend.shared.generate;

import com.backend.shared.models.ReferenceCounter;
import com.backend.shared.repository.ReferenceCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Unified generator for reference numbers across all entities.
 * Supports: ORDER, LEAVE, ATTENDANCE, CHECK_IN
 * Pattern: PREFIX-YYYYMMDD-XXXXXX (unlimited counter per day per entity type)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReferenceNumberGenerator {

    private final ReferenceCounterRepository referenceCounterRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public enum EntityType {
        ORDER("ORD"),
        LEAVE("LEV"),
        ATTENDANCE("ATT"),
        CHECK_IN("CHK");

        private final String prefix;

        EntityType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    /**
     * Generate a reference number for the specified entity type.
     * Format: PREFIX-YYYYMMDD-XXXXXX (e.g., ORD-20260313-000001)
     *
     * @param entityType The type of entity
     * @return Generated reference number
     */
    @Transactional
    public String generateReferenceNumber(EntityType entityType) {
        LocalDate today = LocalDate.now();

        // Get or create counter for entity type and date
        ReferenceCounter counter = referenceCounterRepository
                .findByEntityTypeAndCounterDate(entityType.name(), today)
                .orElseGet(() -> {
                    ReferenceCounter newCounter = new ReferenceCounter();
                    newCounter.setEntityType(entityType.name());
                    newCounter.setCounterDate(today);
                    newCounter.setCounterValue(0L);
                    return referenceCounterRepository.save(newCounter);
                });

        // Increment counter
        counter.setCounterValue(counter.getCounterValue() + 1);
        ReferenceCounter savedCounter = referenceCounterRepository.save(counter);

        String date = today.format(DATE_FORMATTER);
        return String.format("%s-%s-%06d", entityType.getPrefix(), date, savedCounter.getCounterValue());
    }

    /**
     * Generate reference number for ORDER entity.
     *
     * @return Order reference number (ORD-YYYYMMDD-XXXXXX)
     */
    @Transactional
    public String generateOrderNumber() {
        return generateReferenceNumber(EntityType.ORDER);
    }

    /**
     * Generate reference number for LEAVE entity.
     *
     * @return Leave reference number (LEV-YYYYMMDD-XXXXXX)
     */
    @Transactional
    public String generateLeaveNumber() {
        return generateReferenceNumber(EntityType.LEAVE);
    }

    /**
     * Generate reference number for ATTENDANCE entity.
     *
     * @return Attendance reference number (ATT-YYYYMMDD-XXXXXX)
     */
    @Transactional
    public String generateAttendanceNumber() {
        return generateReferenceNumber(EntityType.ATTENDANCE);
    }

    /**
     * Generate reference number for ATTENDANCE CHECK-IN entity.
     *
     * @return Check-in reference number (CHK-YYYYMMDD-XXXXXX)
     */
    @Transactional
    public String generateCheckInNumber() {
        return generateReferenceNumber(EntityType.CHECK_IN);
    }
}
