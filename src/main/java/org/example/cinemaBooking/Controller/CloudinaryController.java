package org.example.cinemaBooking.Controller;

import lombok.RequiredArgsConstructor;
import org.example.cinemaBooking.DTO.Response.Cloudinary.CloudinaryResponse;
import org.example.cinemaBooking.Service.Cloudinary.CloudinaryService;
import org.example.cinemaBooking.Shared.constant.ApiPaths;
import org.example.cinemaBooking.Shared.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Cloudinary.BASE)
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    // ==================== UPLOAD IMAGE ====================
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CloudinaryResponse> uploadImage(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        CloudinaryResponse response = cloudinaryService.uploadImage(file);
        return ApiResponse.<CloudinaryResponse>builder()
                .success(true)
                .message("Image uploaded successfully")
                .data(response)
                .build();
    }

    // ==================== UPLOAD VIDEO ====================
    @PostMapping(value = "/upload/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CloudinaryResponse> uploadVideo(
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        CloudinaryResponse response = cloudinaryService.uploadVideo(file);
        return ApiResponse.<CloudinaryResponse>builder()
                .success(true)
                .message("Video uploaded successfully")
                .data(response)
                .build();
    }

    // ==================== DELETE ====================
    @DeleteMapping("/delete")
    public ApiResponse<String> deleteFile(
            @RequestParam String publicId,
            @RequestParam String resourceType
    ) throws IOException {

        cloudinaryService.deleteFile(publicId, resourceType);
        return ApiResponse.<String>builder()
                .success(true)
                .message("File deleted successfully")
                .data(publicId)
                .build();
    }
}
