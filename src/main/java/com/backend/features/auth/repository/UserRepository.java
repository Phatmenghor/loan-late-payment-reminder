package com.backend.features.auth.repository;

import com.backend.enums.user.AccountStatus;
import com.backend.enums.user.UserType;
import com.backend.features.auth.models.User;
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
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUserIdentifierAndIsDeletedFalse(String userIdentifier);

    @Deprecated
    boolean existsByUserIdentifierAndIsDeletedFalse(String userIdentifier);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.userIdentifier = :userIdentifier AND u.userType = :userType AND u.isDeleted = false")
    boolean existsByUserIdentifierAndUserTypeAndIsDeletedFalse(
            @Param("userIdentifier") String userIdentifier, @Param("userType") UserType userType);

    @Query("SELECT u FROM User u WHERE u.userIdentifier = :userIdentifier AND u.userType = :userType AND u.isDeleted = false")
    Optional<User> findByUserIdentifierAndUserTypeAndIsDeletedFalse(
            @Param("userIdentifier") String userIdentifier, @Param("userType") UserType userType);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findByIdAndIsDeletedFalse(@Param("id") UUID id);

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN u.profile p " +
            "WHERE u.isDeleted = false " +
            "AND (:userTypes IS NULL OR u.userType IN :userTypes) " +
            "AND (:accountStatuses IS NULL OR u.accountStatus IN :accountStatuses) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "    LOWER(u.userIdentifier) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "    LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "    LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "    LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(
            @Param("userTypes") List<UserType> userTypes,
            @Param("accountStatuses") List<AccountStatus> accountStatuses,
            @Param("roles") List<String> roles,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findByRoleAndIsDeletedFalse(@Param("role") String role);

    @Query("SELECT u FROM User u WHERE u.accountStatus = 'ACTIVE' AND u.isDeleted = false")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findAllByIsDeletedFalse();

    /**
     * Count all non-deleted users (optimized for dashboard)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countAllByIsDeletedFalse();

    /**
     * Count users created in current month (optimized for dashboard)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false " +
           "AND YEAR(u.createdAt) = YEAR(CURRENT_DATE) " +
           "AND MONTH(u.createdAt) = MONTH(CURRENT_DATE)")
    long countNewCustomersThisMonth();
}
