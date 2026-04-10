package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Product.CreateProductRequest;
import org.example.cinemaBooking.DTO.Request.Product.UpdateProductRequest;
import org.example.cinemaBooking.DTO.Response.Booking.BookingResponse;
import org.example.cinemaBooking.DTO.Response.Product.ProductResponse;
import org.example.cinemaBooking.Entity.BookingProduct;
import org.example.cinemaBooking.Entity.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "active", ignore = true)
    Product toEntity(CreateProductRequest request);

    ProductResponse toResponse(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Product product, UpdateProductRequest request);

    @Mapping(target = "subtotal",
            expression = "java(p.getItemPrice().multiply(java.math.BigDecimal.valueOf(p.getQuantity())))")
    BookingResponse.ProductInfo toProductInfo(BookingProduct p);

    List<BookingResponse.ProductInfo> toProductInfos(List<BookingProduct> products);
}