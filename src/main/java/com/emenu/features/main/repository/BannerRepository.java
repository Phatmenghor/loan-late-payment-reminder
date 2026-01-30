package com.emenu.features.main.repository;

import com.emenu.enums.common.Status;
import com.emenu.features.main.models.Banner;
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
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    /**
     * Finds a non-deleted banner by ID
     */
    Optional<Banner> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Finds a non-deleted banner by ID with business details eagerly fetched
     */
    @Query("SELECT b FROM Banner b " +
            "WHERE b.id = :id AND b.isDeleted = false")
    Optional<Banner> findByIdWithBusiness(@Param("id") UUID id);


    /**
     * Find all banners with dynamic filtering - paginated
     */
    @Query("SELECT DISTINCT b FROM Banner b " +
            "WHERE b.isDeleted = false " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(b.linkUrl) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Banner> findAllWithFilters(
            @Param("status") Status status,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Find all banners with dynamic filtering - non-paginated
     */
    @Query("SELECT DISTINCT b FROM Banner b " +
            "WHERE b.isDeleted = false " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(b.linkUrl) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Banner> findAllListWithFilters(
            @Param("status") Status status,
            @Param("search") String search,
            Sort sort
    );
}