package com.backend.shared.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "reference_counters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"entity_type", "counter_date"}, name = "uk_reference_counter")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // "ORDER", "LEAVE", "ATTENDANCE", "CHECK_IN"

    @Column(name = "counter_date", nullable = false)
    private LocalDate counterDate;

    @Column(name = "counter_value", nullable = false)
    private Long counterValue = 0L;
}
