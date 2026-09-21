package org.example.wayveesystem.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.configuration.CloudinaryProperties;
import org.example.wayveesystem.dto.response.ImageUploadResponse;
import org.example.wayveesystem.service.CloudinaryImageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageServiceImpl implements CloudinaryImageService {

    private static final String IMAGE_PREFIX = "image/";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Override
    public ImageUploadResponse uploadLocationImage(MultipartFile file) {
        validateImage(file);

        try {
            String folder = cloudinaryProperties.getLocationFolder() != null
                    ? cloudinaryProperties.getLocationFolder()
                    : "wayvee/locations";

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            if (secureUrl == null || publicId == null) {
                throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }

            return new ImageUploadResponse(secureUrl, publicId);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            log.error("Cloudinary image upload failed", exception);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMAGE_REQUIRED);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException(ErrorCode.IMAGE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(IMAGE_PREFIX)) {
            throw new AppException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }
}
