package com.emenu.features.main.dto.filter;

import com.emenu.enums.common.Status;
import com.emenu.shared.dto.BaseAllFilterRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class CategoryAllFilterRequest extends BaseAllFilterRequest {
    private Status status;
}