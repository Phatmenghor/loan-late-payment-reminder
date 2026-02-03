package com.emenu.features.main.repository;

import com.emenu.enums.payment.PaymentMethodType;
import com.emenu.features.main.models.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isDeleted = false AND pm.isActive = true ORDER BY pm.sortOrder ASC, pm.createdAt ASC")
    List<PaymentMethod> findAllActive();

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isDeleted = false ORDER BY pm.sortOrder ASC, pm.createdAt ASC")
    List<PaymentMethod> findAllNotDeleted();

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.id = :id AND pm.isDeleted = false")
    Optional<PaymentMethod> findByIdNotDeleted(@Param("id") UUID id);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.id = :id AND pm.isDeleted = false AND pm.isActive = true")
    Optional<PaymentMethod> findByIdActiveNotDeleted(@Param("id") UUID id);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isDeleted = false " +
            "AND (:search IS NULL OR :search = '' OR LOWER(pm.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(pm.bankName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:type IS NULL OR pm.type = :type) " +
            "AND (:isActive IS NULL OR pm.isActive = :isActive)")
    Page<PaymentMethod> findAllWithFilter(
            @Param("search") String search,
            @Param("type") PaymentMethodType type,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    @Query("SELECT COUNT(pm) > 0 FROM PaymentMethod pm WHERE pm.type = :type AND pm.isDeleted = false AND pm.isActive = true")
    boolean existsActiveByType(@Param("type") PaymentMethodType type);
}
