package com.emenu.config;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.UserRepository;
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

    @Value("${app.init.create-admin:true}")
    private boolean createDefaultAdmin;

    @Value("${app.init.admin-email:phatmenghor19@gmail.com}")
    private String defaultAdminEmail;

    @Value("${app.init.admin-password:88889999}")
    private String defaultAdminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
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
                log.info("Starting data initialization...");

                if (createDefaultAdmin) {
                    int usersCreated = initializeDefaultUsers();
                    log.info("Default users initialization completed - {} users processed", usersCreated);
                }

                initialized.set(true);
                log.info("Data initialization completed successfully!");

            } catch (Exception e) {
                log.error("Error during data initialization: {}", e.getMessage(), e);
                throw new RuntimeException("Data initialization failed", e);
            }
        }
    }

    private int initializeDefaultUsers() {
        try {
            log.info("Initializing default users...");
            return createDeveloperAdmin();
        } catch (Exception e) {
            log.error("Error initializing default users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize default users", e);
        }
    }

    private int createDeveloperAdmin() {
        try {
            String adminUserIdentifier = defaultAdminEmail;

            if (!userRepository.existsByUserIdentifierAndIsDeletedFalse(adminUserIdentifier)) {
                User admin = new User();
                admin.setUserIdentifier(adminUserIdentifier);
                admin.setEmail(defaultAdminEmail);
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                admin.setFirstName("Platform");
                admin.setLastName("Administrator");
                admin.setAccountStatus(AccountStatus.ACTIVE);
                admin.setRole(RoleEnum.DEVELOPER);

                admin = userRepository.save(admin);
                log.info("Created developer admin: {} with ID: {}", adminUserIdentifier, admin.getId());
                return 1;
            } else {
                log.info("Developer admin already exists: {}", adminUserIdentifier);
                return 0;
            }
        } catch (Exception e) {
            log.error("Error creating developer admin: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create developer admin", e);
        }
    }
}
