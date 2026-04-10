package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.Entity.RoleEntity;
import org.example.cinemaBooking.Entity.UserEntity;
import org.example.cinemaBooking.DTO.Request.User.RegisterRequest;
import org.example.cinemaBooking.DTO.Request.User.UpdateProfileRequest;
import org.example.cinemaBooking.DTO.Response.User.UserInfoResponse;
import org.example.cinemaBooking.DTO.Response.User.UserResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserInfoResponse toUserInfoResponse(UserEntity userEntity);

    UserEntity toUserEntity(RegisterRequest registerRequest);

    UserResponse toUserResponse(UserEntity userEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(UpdateProfileRequest request, @MappingTarget UserEntity user);

    default String map(RoleEntity roleEntity) {
        return roleEntity.getName();
    }
}
