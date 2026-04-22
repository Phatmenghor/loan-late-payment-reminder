package com.backend.shared.repository;

import com.backend.shared.models.ReferenceCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReferenceCounterRepository extends JpaRepository<ReferenceCounter, Long> {

    Optional<ReferenceCounter> findByEntityTypeAndCounterDate(String entityType, LocalDate counterDate);

    @Modifying
    @Query("UPDATE ReferenceCounter SET counterValue = counterValue + 1 WHERE entityType = :entityType AND counterDate = :date")
    int incrementCounterForEntityAndDate(String entityType, LocalDate date);
}
