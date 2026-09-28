package dev.franklin.salalivre;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import dev.franklin.salalivre.reservation.ReservationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ReservationRepository reservations;

    @BeforeEach
    void cleanReservations() {
        reservations.deleteAll();
    }

    private String payload(long roomId, LocalDate day, String start, String end) throws Exception {
        return mapper.writeValueAsString(new BookingInput(
            roomId, "Reunião de planejamento", "Marina Costa",
            LocalDateTime.of(day, LocalTime.parse(start)),
            LocalDateTime.of(day, LocalTime.parse(end))
        ));
    }

    private long create(long roomId, LocalDate day, String start, String end) throws Exception {
        String response = mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(roomId, day, start, end)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asLong();
    }

    @Test
    void roomsAreAvailableFromTheSeedMigration() throws Exception {
        mvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(4)))
            .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void overlappingBookingsAreRejectedButAdjacentSlotsAreAllowed() throws Exception {
        LocalDate day = LocalDate.now().plusDays(1);
        create(1, day, "09:00", "10:00");

        mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(1, day, "09:30", "10:30")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "Esta sala já tem uma reserva nesse horário. Escolha outro intervalo."));

        create(1, day, "10:00", "11:00");
        create(2, day, "09:00", "10:00");
        mvc.perform(get("/api/reservations").param("date", day.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void cancellingKeepsHistoryAndFreesTheSlot() throws Exception {
        LocalDate day = LocalDate.now().plusDays(2);
        long id = create(1, day, "13:00", "14:00");
        mvc.perform(patch("/api/reservations/{id}/cancel", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        create(1, day, "13:00", "14:00");
        mvc.perform(get("/api/reservations").param("date", day.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void invalidTimesAndMissingResourcesReturnUsefulStatusCodes() throws Exception {
        LocalDate day = LocalDate.now().plusDays(1);
        mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(1, day, "09:00", "09:10")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(1, day, "06:00", "07:00")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(999, day, "09:00", "10:00")))
            .andExpect(status().isNotFound());
        mvc.perform(patch("/api/reservations/999/cancel"))
            .andExpect(status().isNotFound());
    }

    private record BookingInput(Long roomId, String title, String requester,
                                LocalDateTime startsAt, LocalDateTime endsAt) {
    }
}
