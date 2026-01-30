package com.emenu.features.auth.dto.response;

import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleResponse extends BaseAuditResponse {
    private String name;
    private String description;
    private UUID businessId;
}
