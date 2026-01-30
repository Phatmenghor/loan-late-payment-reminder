package com.emenu.features.auth.controller;

import com.emenu.features.auth.dto.filter.RoleFilterRequest;
import com.emenu.features.auth.dto.request.RoleCreateRequest;
import com.emenu.features.auth.dto.response.RoleDetailResponse;
import com.emenu.features.auth.dto.response.RoleResponse;
import com.emenu.features.auth.dto.update.RoleUpdateRequest;
import com.emenu.features.auth.service.RoleService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    /**
     * Create a new role (platform-level or business-specific)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody RoleCreateRequest request) {
        log.info("Create role request: {}", request.getName());
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Role created successfully", response));
    }

    /**
     * Get all roles with filtering and pagination
     * Filter options: businessId, userTypes, search, includeAll
     */
    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<RoleResponse>>> getAllRoles(
            @Valid @RequestBody RoleFilterRequest request) {
        log.info("Get all roles with filters and pagination");
        PaginationResponse<RoleResponse> response = roleService.getAllRoles(request);
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", response));
    }

    /**
     * Get all roles as list with filtering (no pagination)
     * Filter options: businessId, userTypes, search, includeAll
     */
    @PostMapping("/all-list")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRolesList(
            @Valid @RequestBody RoleFilterRequest request) {
        log.info("Get all roles as list with filters");
        List<RoleResponse> response = roleService.getAllRolesList(request);
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", response));
    }

    /**
     * Get a role by ID
     */
    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleDetailResponse>> getRoleById(
            @PathVariable UUID roleId) {
        log.info("Get role by ID: {}", roleId);
        RoleDetailResponse response = roleService.getRoleById(roleId);
        return ResponseEntity.ok(ApiResponse.success("Role retrieved", response));
    }

    /**
     * Update a role
     * Cannot update system roles (PLATFORM_OWNER, BUSINESS_OWNER, CUSTOMER)
     */
    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("Update role: {}", roleId);
        RoleResponse response = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", response));
    }

    /**
     * Delete a role (soft delete)
     * Cannot delete system roles
     * Does not affect current users - owners can update users themselves
     */
    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleResponse>> deleteRole(
            @PathVariable UUID roleId) {
        log.info("Delete role: {}", roleId);
        RoleResponse response = roleService.deleteRole(roleId);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", response));
    }
}
