package com.backend.features.setting.service.impl;

import com.backend.exception.custom.NotFoundException;
import com.backend.features.setting.dto.response.ImageDto;
import com.backend.features.setting.dto.response.ImageResponse;
import com.backend.features.setting.dto.request.ImageUploadRequest;
import com.backend.features.setting.mapper.ImageMapper;
import com.backend.features.setting.models.ImageEntity;
import com.backend.features.setting.repository.ImageRepository;
import com.backend.features.setting.service.ImageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    @Override
    @Transactional
    public ImageDto uploadImage(ImageUploadRequest request) {
        // Normalize the type field - add "image/" prefix if it doesn't have it
        if (!request.getType().contains("/")) {
            request.setType("image/" + request.getType());
        }

        ImageEntity image = imageMapper.toEntity(request);
        ImageEntity savedImage = imageRepository.save(image);
        return imageMapper.toDto(savedImage);
    }
    
    @Override
    @Transactional()
    public ImageResponse getImageById(UUID id) {
        ImageEntity image = imageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Image not found with id: " + id));
        return imageMapper.toResponse(image);
    }

    
    @Override
    @Transactional
    public void deleteImage(UUID id) {
        if (!imageRepository.existsById(id)) {
            throw new NotFoundException("Image not found with id: " + id);
        }
        imageRepository.deleteById(id);
    }
}