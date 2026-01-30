package com.emenu.features.main.dto.filter;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseFilterRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class BannerFilterRequest extends BaseFilterRequest {
    private Status status;
}

