package com.emenu.features.main.dto.response;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseAuditResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class BannerResponse extends BaseAuditResponse {
    private String imageUrl;
    private String linkUrl;
    private Status status;
}
