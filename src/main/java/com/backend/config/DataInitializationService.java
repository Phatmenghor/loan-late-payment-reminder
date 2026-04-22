package com.backend.config;

import com.backend.enums.user.AccountStatus;
import com.backend.enums.user.UserRole;
import com.backend.enums.user.UserType;
import com.backend.features.auth.models.User;
import com.backend.features.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final Object initLock = new Object();
    private static final String PLACEHOLDER_IMAGE_URL = "https://plus.unsplash.com/premium_photo-1673002094195-f18084be89ce";

    @Value("${app.init.create-admin:true}")
    private boolean createDefaultAdmin;

    @Value("${app.init.admin-email:phatmenghor19@gmail.com}")
    private String defaultAdminEmail;

    @Value("${app.init.admin-password:88889999}")
    private String defaultAdminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
        // ✅ ENHANCED: Double-checked locking pattern for thread safety
        if (initialized.get()) {
            log.info("Data initialization already completed. Skipping...");
            return;
        }

        synchronized (initLock) {
            if (initialized.get()) {
                log.info("Data initialization already completed (double-check). Skipping...");
                return;
            }

            try {
                log.info("🚀 Starting Cambodia E-Menu Platform data initialization...");

                if (createDefaultAdmin) {
                    int usersCreated = initializeDefaultUsers();
                    log.info("✅ Default users initialization completed - {} users processed", usersCreated);
                }

                // Mark as initialized only after all steps complete
                initialized.set(true);
                log.info("🎉 Cambodia E-Menu Platform data initialization completed successfully!");

            } catch (Exception e) {
                log.error("❌ Error during data initialization: {}", e.getMessage(), e);
                // Don't set initialized flag on failure so it can be retried
                throw new RuntimeException("Data initialization failed", e);
            }
        }
    }

    private int initializeDefaultUsers() {
        try {
            log.info("🔄 Initializing default users...");
            
            int usersCreated = 0;
            usersCreated += createPlatformOwner();
            
            return usersCreated;
            
        } catch (Exception e) {
            log.error("❌ Error initializing default users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize default users", e);
        }
    }

    private int createPlatformOwner() {
        try {
            String adminUserIdentifier = defaultAdminEmail;

            if (!userRepository.existsByUserIdentifierAndIsDeletedFalse(adminUserIdentifier)) {
                User admin = new User();
                admin.setUserIdentifier(adminUserIdentifier);
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                admin.setUserType(UserType.OWNER);
                admin.setUserRole(UserRole.ADMIN);
                admin.setAccountStatus(AccountStatus.ACTIVE);

                admin = userRepository.save(admin);

                com.backend.features.auth.models.UserProfile profile = new com.backend.features.auth.models.UserProfile();
                profile.setUser(admin);
                profile.setEmail(defaultAdminEmail);
                profile.setFirstName("Platform");
                profile.setLastName("Administrator");
                profile.setProfileImageUrl(PLACEHOLDER_IMAGE_URL);
                admin.setProfile(profile);

                admin = userRepository.save(admin);
                log.info("✅ Created platform owner: {} with ID: {}", adminUserIdentifier, admin.getId());
                return 1;
            } else {
                log.info("ℹ️ Platform owner already exists: {}", adminUserIdentifier);
                return 0;
            }
        } catch (Exception e) {
            log.error("❌ Error creating platform owner: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create platform owner", e);
        }
    }
}