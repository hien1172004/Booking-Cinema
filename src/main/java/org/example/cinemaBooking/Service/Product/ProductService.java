package org.example.cinemaBooking.Service.Product;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Product.CreateProductRequest;
import org.example.cinemaBooking.DTO.Request.Product.UpdateProductRequest;
import org.example.cinemaBooking.DTO.Response.Product.ProductResponse;
import org.example.cinemaBooking.Entity.Product;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ProductMapper;
import org.example.cinemaBooking.Repository.ProductRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request){
        Optional<Product> existingProduct = productRepository.findByName(request.name());

        if(existingProduct.isPresent()){
           Product product = existingProduct.get();
           if(product.isDeleted()){
               product.restore();
               product.setImage(request.image());
               product.setActive(true);
               product.setPrice(request.price());
               Product savedProduct = productRepository.save(product);
               return productMapper.toResponse(savedProduct);
           }
           throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(String id, UpdateProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if(request.name() != null && !request.name().isEmpty()){
            Optional<Product> existingProduct = productRepository.findByName(request.name());
            if(existingProduct.isPresent() && !existingProduct.get().getId().equals(id)){
                throw new AppException(ErrorCode.PRODUCT_ALREADY_EXISTS);
            }
            product.setName(request.name());
        }

        productMapper.update(product, request);

        return productMapper.toResponse(product);
    }

    @Transactional
    public void deleteProduct(String id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.softDelete();
        product.setActive(false);
        productRepository.save(product);
    }

    public ProductResponse getProductById(String id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    public void toggleActiveProduct(String id){
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setActive(!product.getActive());
        productRepository.save(product);
    }

    public PageResponse<ProductResponse> getAllProducts(
            int page,
            int size,
            String sortBy,
            String direction,
            String keyword
    ) {
        int pageNumber = page > 0 ? page -1 : 0;

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, size, sort);

        Page<Product> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productRepository
                    .findByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable);
        } else {
            productPage = productRepository.findAllByDeletedFalse(pageable);
        }

        return PageResponse.<ProductResponse>builder()
                .page(page)
                .size(size)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .items(productPage.getContent()
                        .stream()
                        .map(productMapper::toResponse)
                        .toList())
                .build();
    }

    public PageResponse<ProductResponse> getProductActive(
            int page,
            int size,
            String sortBy,
            String direction) {

        int pageNumber = page > 0 ? page -1 : 0;
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortBy));
        Page<Product> productPage = productRepository.findByActiveTrueAndDeletedFalse(pageable);

        List<ProductResponse> productResponses = productPage.getContent()
                .stream()
                .map(productMapper::toResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .page(page)
                .size(size)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .items(productResponses)
                .build();
    }
}
