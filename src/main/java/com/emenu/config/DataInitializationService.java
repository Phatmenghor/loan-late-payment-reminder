package com.emenu.config;

import com.emenu.enums.user.AccountStatus;
import com.emenu.features.auth.models.Role;
import com.emenu.features.auth.models.User;
import com.emenu.features.auth.repository.RoleRepository;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializationService {

    private final RoleRepository roleRepository;
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

                // Initialize in strict order
                int rolesCreated = ensureRolesExist();
                log.info("✅ Roles initialization completed - {} roles processed", rolesCreated);


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

    private int ensureRolesExist() {
        try {
            log.info("🔄 Ensuring system roles exist...");

            // System roles with their user types
            record RoleConfig(String name, UserType userType) {}
            RoleConfig[] systemRoles = {
                    new RoleConfig("PLATFORM_OWNER", UserType.PLATFORM_USER),
                    new RoleConfig("BUSINESS_OWNER", UserType.BUSINESS_USER),
                    new RoleConfig("CUSTOMER", UserType.CUSTOMER)
            };
            int createdCount = 0;

            for (RoleConfig roleConfig : systemRoles) {
                if (!roleRepository.existsByNameAndIsDeletedFalse(roleConfig.name())) {
                    Role role = new Role();
                    role.setName(roleConfig.name());
                    role.setDescription("System role: " + roleConfig.name());
                    role.setBusinessId(null);
                    role.setUserType(roleConfig.userType());
                    roleRepository.save(role);
                    createdCount++;
                    log.info("✅ Created system role: {} for user type: {}", roleConfig.name(), roleConfig.userType());
                }
            }

            if (createdCount > 0) {
                log.info("✅ Created {} system roles", createdCount);
            } else {
                log.info("✅ All system roles already exist");
            }

            return systemRoles.length;

        } catch (Exception e) {
            log.error("❌ Error during roles verification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to ensure roles exist", e);
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
                admin.setEmail(defaultAdminEmail);
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                admin.setFirstName("Platform");
                admin.setLastName("Administrator");
                admin.setUserType(UserType.PLATFORM_USER);
                admin.setPosition("Platform Owner");
                admin.setAccountStatus(AccountStatus.ACTIVE);

                Role platformOwnerRole = roleRepository.findByNameAndIsDeletedFalse("PLATFORM_OWNER")
                        .orElseThrow(() -> new RuntimeException("Platform owner role not found"));
                admin.setRoles(List.of(platformOwnerRole));

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