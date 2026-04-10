package org.example.cinemaBooking.Service.Cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.cinemaBooking.DTO.Response.Cloudinary.CloudinaryResponse;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;




@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // ==================== CONSTANTS ====================
    private static final String RESOURCE_TYPE = "resource_type";
    private static final String IMAGE = "image";
    private static final String VIDEO = "video";
    private static final String FOLDER = "folder";
    private static final String IMAGES_FOLDER = "images";
    private static final String VIDEOS_FOLDER = "videos";

    private static final String SECURE_URL = "secure_url";
    private static final String PUBLIC_ID = "public_id";
    private static final String FORMAT = "format";

    // ==================== UPLOAD IMAGE ====================
    public CloudinaryResponse uploadImage(MultipartFile file) {
        validateImage(file);

        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    RESOURCE_TYPE, IMAGE,
                    FOLDER, IMAGES_FOLDER,
                    "unique_filename", true
            );

            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(), // 🔥 FIX CHUẨN
                    options
            );

            return mapToResponse(result);

        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ==================== UPLOAD VIDEO ====================
    public CloudinaryResponse uploadVideo(MultipartFile file) {
        validateVideo(file);

        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    RESOURCE_TYPE, VIDEO,
                    FOLDER, VIDEOS_FOLDER,
                    "chunk_size", 6000000
            );

            Map<String, Object> result = cloudinary.uploader().uploadLarge(
                    file.getBytes(), // 🔥 FIX
                    options
            );

            return mapToResponse(result);

        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    // ==================== DELETE ====================
    public void deleteFile(String publicId, String resourceType) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(RESOURCE_TYPE, resourceType)
            );

            if (!"ok".equals(result.get("result"))) {
                throw new AppException(ErrorCode.FILE_DELETE_FAILED);
            }

        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    // ==================== VALIDATION ====================
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.FILE_TYPE_INVALID);
        }
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new AppException(ErrorCode.FILE_TYPE_INVALID);
        }
    }

    // ==================== RESPONSE ====================
    private CloudinaryResponse mapToResponse(Map<String, Object> result) {
        return CloudinaryResponse.builder()
                .url((String) result.get(SECURE_URL))
                .publicId((String) result.get(PUBLIC_ID))
                .format((String) result.get(FORMAT))
                .resourceType((String) result.get(RESOURCE_TYPE))
                .build();
    }
}