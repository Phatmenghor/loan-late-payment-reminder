package com.emenu.features.auth.repository;

import com.emenu.features.auth.models.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {

    @Query("SELECT ul FROM UserLocation ul " +
            "WHERE ul.userId = :userId AND ul.isDeleted = false " +
            "ORDER BY ul.isPrimary DESC, ul.createdAt DESC")
    List<UserLocation> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT ul FROM UserLocation ul " +
            "WHERE ul.id = :id AND ul.userId = :userId AND ul.isDeleted = false")
    Optional<UserLocation> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT ul FROM UserLocation ul " +
            "WHERE ul.userId = :userId AND ul.isPrimary = true AND ul.isDeleted = false")
    Optional<UserLocation> findPrimaryByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserLocation ul SET ul.isPrimary = false " +
            "WHERE ul.userId = :userId AND ul.isDeleted = false")
    void clearPrimaryByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ul) FROM UserLocation ul " +
            "WHERE ul.userId = :userId AND ul.isDeleted = false")
    long countByUserId(@Param("userId") UUID userId);
}
