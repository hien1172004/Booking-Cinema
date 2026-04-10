package org.example.cinemaBooking.Service.Movie;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Movie.CreateMovieImageRequest;
import org.example.cinemaBooking.DTO.Request.Movie.UpdateMovieImageRequest;
import org.example.cinemaBooking.DTO.Response.Movie.MovieImageResponse;
import org.example.cinemaBooking.Entity.Movie;
import org.example.cinemaBooking.Entity.MovieImage;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.MovieImageMapper;
import org.example.cinemaBooking.Repository.MovieImageRepository;
import org.example.cinemaBooking.Repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class MovieImageService {

    MovieRepository movieRepository;
    MovieImageRepository movieImageRepository;
    MovieImageMapper movieImageMapper;


    @Transactional
    public List<MovieImageResponse> createMovieImage(String movieId, CreateMovieImageRequest request) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        // Lấy danh sách URLs đã tồn tại trong DB
        List<MovieImage> existingImages = movieImageRepository.findByMovieId(movieId);
        Set<String> existingUrls = existingImages.stream()
                .map(MovieImage::getImageUrl)
                .collect(Collectors.toSet());

        // Lọc URLs mới (không tồn tại trong DB)
        Set<String> uniqueNewUrls = new HashSet<>(request.getImageUrls());
        List<String> newUrls = uniqueNewUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .toList();


        log.info("Request URLs: {}", request.getImageUrls());
        log.info("Existing URLs: {}", existingUrls);
        log.info("New URLs to add: {}", newUrls);

        // Nếu không có ảnh mới, trả về danh sách ảnh hiện tại
        if (newUrls.isEmpty()) {
            log.info("No new images to add for movieId: {}", movieId);
            return existingImages.stream()
                    .map(movieImageMapper::toResponse)
                    .toList();
        }

        // Chỉ tạo ảnh cho URLs mới
        List<MovieImage> movieImages = newUrls.stream()
                .map(url -> MovieImage.builder()
                        .imageUrl(url)
                        .movie(movie)
                        .build())
                .toList();

        List<MovieImage> savedImages = movieImageRepository.saveAll(movieImages);
        movieImageRepository.flush();

        // Kết hợp ảnh cũ và ảnh mới
        List<MovieImage> allImages = new ArrayList<>();
        allImages.addAll(existingImages);
        allImages.addAll(savedImages);

        log.info("[MOVIE_IMAGE_SERVICE] action=create movieId={} new={} total={}",
                movieId, savedImages.size(), allImages.size());

        return allImages.stream()
                .map(movieImageMapper::toResponse)
                .toList();
    }


    @Transactional
    public void updateMovieImage(String movieId, UpdateMovieImageRequest request) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        // remove duplicate input
        Set<String> newUrls = new HashSet<>(request.getImageUrls());

        // fetch current images
        List<MovieImage> existingImages = movieImageRepository.findByMovieId(movieId);

        Set<String> existingUrls = existingImages.stream()
                .map(MovieImage::getImageUrl)
                .collect(Collectors.toSet());

        // ===== DELETE =====
        List<MovieImage> toDelete = existingImages.stream()
                .filter(img -> !newUrls.contains(img.getImageUrl()))
                .toList();

        if (!toDelete.isEmpty()) {
            movieImageRepository.deleteAllInBatch(toDelete);
        }


        List<MovieImage> toAdd = newUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .map(url -> MovieImage.builder()
                        .imageUrl(url)
                        .movie(movie)
                        .build())
                .toList();

        if (!toAdd.isEmpty()) {
            movieImageRepository.saveAll(toAdd);
        }

        log.info("[MOVIE_IMAGE_SERVICE] action=update movieId={} add={} delete={}",
                movieId, toAdd.size(), toDelete.size());
    }


    @Transactional
    public void deleteMovieImage(String movieId, String imageId) {

        MovieImage movieImage = movieImageRepository
                .findByIdAndMovieId(imageId, movieId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_IMAGE_NOT_FOUND));

        movieImageRepository.delete(movieImage);

        log.info("[MOVIE_IMAGE_SERVICE] action=delete movieId={} imageId={}",
                movieId, imageId);
    }

    public List<MovieImageResponse> getMovieImageByMovieId(String movieId) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(ErrorCode.MOVIE_NOT_FOUND));

        List<MovieImage> movieImages = movieImageRepository.findByMovieId(movie.getId());

        return movieImages.stream()
                .map(movieImageMapper::toResponse)
                .toList();
    }

}