package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.response.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryImageService {
    ImageUploadResponse uploadLocationImage(MultipartFile file);
}
