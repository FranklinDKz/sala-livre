package dev.franklin.salalivre.room;

public record RoomResponse(Long id, String name, String location, int capacity, String equipment) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getLocation(),
                                room.getCapacity(), room.getEquipment());
    }
}
