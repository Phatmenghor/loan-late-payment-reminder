package com.backend.features.auth.dto.filter;

import com.backend.enums.user.AccountStatus;
import com.backend.enums.user.UserRole;
import com.backend.enums.user.UserType;
import com.backend.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserFilterRequest extends BaseFilterRequest {
    private List<AccountStatus> accountStatuses;
    private List<UserType> userTypes;
    private List<UserRole> userRoles;
}