package com.backend.features.auth.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extended user response with full nested details
 * Used for detail views to include profile information
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDetailResponse extends UserResponse {
}
