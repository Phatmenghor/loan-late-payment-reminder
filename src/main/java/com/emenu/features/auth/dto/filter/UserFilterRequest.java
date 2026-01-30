package com.emenu.features.auth.dto.filter;

import com.emenu.enums.user.AccountStatus;
import com.emenu.enums.user.RoleEnum;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserFilterRequest extends BaseFilterRequest {
    private List<AccountStatus> accountStatuses;
    private List<RoleEnum> roles;
}
