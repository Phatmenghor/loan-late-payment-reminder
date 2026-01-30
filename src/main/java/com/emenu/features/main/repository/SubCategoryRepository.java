package com.emenu.features.main.repository;

import com.emenu.enums.common.Status;
import com.emenu.features.main.models.SubCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubCategoryRepository extends JpaRepository<SubCategory, UUID> {

    @Query("SELECT sc FROM SubCategory sc " +
            "LEFT JOIN FETCH sc.category " +
            "WHERE sc.id = :id AND sc.isDeleted = false")
    Optional<SubCategory> findById(@Param("id") UUID id);

    boolean existsByNameAndCategoryIdAndIsDeletedFalse(String name, UUID categoryId);

    @Query("SELECT DISTINCT sc FROM SubCategory sc " +
            "LEFT JOIN sc.category c " +
            "WHERE sc.isDeleted = false " +
            "AND (:status IS NULL OR sc.status = :status) " +
            "AND (:categoryId IS NULL OR sc.categoryId = :categoryId) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(sc.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SubCategory> findAllWithFilters(
            @Param("status") Status status,
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT DISTINCT sc FROM SubCategory sc " +
            "LEFT JOIN sc.category c " +
            "WHERE sc.isDeleted = false " +
            "AND (:status IS NULL OR sc.status = :status) " +
            "AND (:categoryId IS NULL OR sc.categoryId = :categoryId) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(sc.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<SubCategory> findAllWithFilters(
            @Param("status") Status status,
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Sort sort
    );

    long countByCategoryIdAndIsDeletedFalse(UUID categoryId);
}
