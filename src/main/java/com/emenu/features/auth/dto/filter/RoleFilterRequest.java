package com.emenu.features.auth.dto.filter;

import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleFilterRequest extends BaseFilterRequest {
    private UUID businessId;
    private List<UserType> userTypes;
    private Boolean includeAll = false;
}
