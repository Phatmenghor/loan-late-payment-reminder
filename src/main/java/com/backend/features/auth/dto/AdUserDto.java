package com.backend.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdUserDto {
    private String samaccountName;
    private String displayName;
    private String cn;
    private String givenName;
    private String sn;
    private String mail;
    private String department;
    private String title;
    private String telephoneNumber;
    private String mobile;
    private String company;
    private String distinguishedName;
    private List<String> memberOf;
}
