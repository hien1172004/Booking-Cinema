package org.example.cinemaBooking.DTO.Response.Cloudinary;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudinaryResponse {
    private String url;
    private String publicId;
    private String format;
    private String resourceType;
}