package com.backend.features.auth.service;

import com.backend.features.auth.dto.LoginRequest;
import com.backend.features.auth.dto.LoginResponse;

public interface AdAuthService {

    LoginResponse login(LoginRequest request, String apiKeyHeader);
}
