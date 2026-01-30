package com.emenu.features.auth.dto.response;

import com.emenu.enums.user.AccountStatus;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserResponse extends BaseAuditResponse {
    
    private String userIdentifier;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private String profileImageUrl;
    private AccountStatus accountStatus;
    private List<String> roles;
    private String position;
    private String address;
    private String notes;
    private UUID businessId;
    private String businessName;
}