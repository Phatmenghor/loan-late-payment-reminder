package com.emenu.features.auth.repository;

import com.emenu.features.auth.models.Role;
import org.hibernate.usertype.UserType;
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
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameAndIsDeletedFalse(String name);

    Optional<Role> findByIdAndIsDeletedFalse(UUID id);

    List<Role> findByNameInAndIsDeletedFalse(List<String> names);

    boolean existsByNameAndIsDeletedFalse(String name);

    boolean existsByNameAndBusinessIdAndIsDeletedFalse(String name, UUID businessId);

    boolean existsByNameAndBusinessIdIsNullAndIsDeletedFalse(String name);


    @Query("SELECT r FROM Role r WHERE " +
            "(:includeAll = true OR r.isDeleted = false) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Role> findAllWithFilters(
            @Param("search") String search,
            @Param("includeAll") Boolean includeAll,
            Pageable pageable);

    @Query("SELECT r FROM Role r WHERE " +
            "(:includeAll = true OR r.isDeleted = false) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Role> findAllListWithFilters(
            @Param("search") String search,
            @Param("includeAll") Boolean includeAll);
}
