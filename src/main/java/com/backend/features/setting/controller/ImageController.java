package com.backend.features.setting.controller;

import com.backend.features.setting.dto.response.ImageDto;
import com.backend.features.setting.dto.response.ImageResponse;
import com.backend.features.setting.dto.request.ImageUploadRequest;
import com.backend.features.setting.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {
    
    private final ImageService imageService;

    /**
     * Uploads a new image
     */
    @PostMapping
    public ResponseEntity<ImageDto> uploadImage(@Valid @RequestBody ImageUploadRequest request) {
        log.info("Uploading image");
        ImageDto uploadedImage = imageService.uploadImage(request);
        return new ResponseEntity<>(uploadedImage, HttpStatus.CREATED);
    }

    /**
     * Retrieves image data by ID
     * Returns the image with proper headers for browser display
     */
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImageData(@PathVariable UUID id) {
        log.info("Get image data: {}", id);
        ImageResponse imageResponse = imageService.getImageById(id);
        return ResponseEntity
                .ok()
                .contentType(MediaType.valueOf(imageResponse.getType()))
                .header("Content-Disposition", "inline; filename=\"image\"")
                .header("Cache-Control", "public, max-age=86400")
                .header("Access-Control-Allow-Origin", "*")
                .body(imageResponse.getData());
    }

    /**
     * Deletes an image by its ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id) {
        log.info("Delete image: {}", id);
        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}