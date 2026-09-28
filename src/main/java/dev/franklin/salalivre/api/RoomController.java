package dev.franklin.salalivre.api;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.franklin.salalivre.room.RoomRepository;
import dev.franklin.salalivre.room.RoomResponse;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomRepository rooms;

    public RoomController(RoomRepository rooms) {
        this.rooms = rooms;
    }

    @GetMapping
    public List<RoomResponse> list() {
        return rooms.findAll(Sort.by("name")).stream().map(RoomResponse::from).toList();
    }
}
