package com.backend.features.setting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDto {
    private UUID id;
    private String imageUrl;
    private String type;
}