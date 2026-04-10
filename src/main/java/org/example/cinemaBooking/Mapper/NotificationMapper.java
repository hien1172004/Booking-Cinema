package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Response.Notification.NotificationResponse;
import org.example.cinemaBooking.Entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "notificationId", source = "id")
    NotificationResponse toResponse(Notification n);
}