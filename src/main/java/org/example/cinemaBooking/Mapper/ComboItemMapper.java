package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Combo.ComboItemResponse;
import org.example.cinemaBooking.Entity.ComboItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ComboItemMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    ComboItemResponse toResponse(ComboItem comboItem);

}