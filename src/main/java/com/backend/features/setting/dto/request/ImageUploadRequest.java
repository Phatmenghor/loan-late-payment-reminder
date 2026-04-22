package com.backend.features.setting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageUploadRequest {

    @NotBlank(message = "Image type is required")
    private String type;

    @NotBlank(message = "Image data is required")
    private String base64;
}