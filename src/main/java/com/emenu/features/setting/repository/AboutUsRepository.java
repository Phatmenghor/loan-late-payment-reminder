package com.emenu.features.setting.repository;

import com.emenu.features.setting.models.AboutUs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AboutUsRepository extends JpaRepository<AboutUs, UUID> {

    Optional<AboutUs> findFirstByIsDeletedFalseOrderByCreatedAtAsc();
}
