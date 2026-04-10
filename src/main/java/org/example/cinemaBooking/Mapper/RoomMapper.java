package org.example.cinemaBooking.Mapper;

import org.example.cinemaBooking.DTO.Request.Room.CreateRoomRequest;
import org.example.cinemaBooking.DTO.Request.Room.UpdateRoomRequest;
import org.example.cinemaBooking.DTO.Response.Room.RoomBasicResponse;
import org.example.cinemaBooking.DTO.Response.Room.RoomResponse;
import org.example.cinemaBooking.Entity.Room;
import org.mapstruct.*;

@Mapper(componentModel = "Spring")
public interface RoomMapper {

    // ✅ ENTITY -> RESPONSE
    @Mapping(source = "cinema.id", target = "cinemaId")
    @Mapping(source = "cinema.name", target = "cinemaName")
    RoomResponse toResponse(Room room);

    // ✅ REQUEST -> ENTITY
    @Mapping(target = "cinema", ignore = true) // xử lý ở service
    @Mapping(target = "seats", ignore = true)
    Room toRoomEntity(CreateRoomRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRoom(UpdateRoomRequest request, @MappingTarget Room room);

    RoomBasicResponse toBasicResponse(Room room);

}
