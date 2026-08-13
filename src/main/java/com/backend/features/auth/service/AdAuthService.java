package com.backend.features.auth.service;

import com.backend.features.auth.dto.request.AdSystemUserUnlockRequest;
import com.backend.features.auth.dto.request.LoginRequest;
import com.backend.features.auth.dto.response.AdSystemUserUnlockResponse;
import com.backend.features.auth.dto.response.LoginResponse;

public interface AdAuthService {

    LoginResponse login(LoginRequest request, String apiKeyHeader);

    AdSystemUserUnlockResponse unlockUser(AdSystemUserUnlockRequest request, String apiKeyHeader);

    int lockInactiveUsers(int inactivityDays);
}
