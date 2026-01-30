package com.emenu.features.auth.dto.filter;

import com.emenu.enums.user.AccountStatus;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserFilterRequest extends BaseFilterRequest {
    private UUID businessId;
    private List<AccountStatus> accountStatuses;
    private List<UserType> userTypes;
    private List<String> roles;
}