package dev.franklin.salalivre.reservation;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.franklin.salalivre.api.BookingException;
import dev.franklin.salalivre.room.Room;
import dev.franklin.salalivre.room.RoomRepository;

@Service
public class ReservationService {
    private static final LocalTime OPENING = LocalTime.of(7, 0);
    private static final LocalTime CLOSING = LocalTime.of(22, 0);

    private final ReservationRepository reservations;
    private final RoomRepository rooms;

    public ReservationService(ReservationRepository reservations, RoomRepository rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listForDay(LocalDate date) {
        return reservations.findForDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            .stream().map(ReservationResponse::from).toList();
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest request) {
        validateHours(request);

        // Lock the room row so two simultaneous requests cannot reserve the same slot.
        Room room = rooms.findByIdForBooking(request.roomId())
            .orElseThrow(() -> new BookingException(HttpStatus.NOT_FOUND, "Sala não encontrada."));

        if (reservations.countConflicts(room.getId(), request.startsAt(), request.endsAt()) > 0) {
            throw new BookingException(HttpStatus.CONFLICT,
                "Esta sala já tem uma reserva nesse horário. Escolha outro intervalo.");
        }

        Reservation reservation = new Reservation(
            room, request.title().trim(), request.requester().trim(),
            request.startsAt(), request.endsAt(), LocalDateTime.now()
        );
        return ReservationResponse.from(reservations.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation = reservations.findById(id)
            .orElseThrow(() -> new BookingException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
        reservation.cancel(LocalDateTime.now());
        return ReservationResponse.from(reservation);
    }

    private void validateHours(CreateReservationRequest request) {
        LocalDateTime start = request.startsAt();
        LocalDateTime end = request.endsAt();
        if (!start.isBefore(end) || !start.toLocalDate().equals(end.toLocalDate())) {
            throw new BookingException(HttpStatus.BAD_REQUEST,
                "Início e fim devem estar no mesmo dia, com o fim após o início.");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new BookingException(HttpStatus.BAD_REQUEST, "Não é possível reservar um horário passado.");
        }
        if (start.toLocalTime().isBefore(OPENING) || end.toLocalTime().isAfter(CLOSING)) {
            throw new BookingException(HttpStatus.BAD_REQUEST,
                "As reservas devem ficar entre 07:00 e 22:00.");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 30 || minutes > 480) {
            throw new BookingException(HttpStatus.BAD_REQUEST,
                "A reserva deve durar entre 30 minutos e 8 horas.");
        }
        if (start.getMinute() % 30 != 0 || end.getMinute() % 30 != 0
                || start.getSecond() != 0 || end.getSecond() != 0
                || start.getNano() != 0 || end.getNano() != 0) {
            throw new BookingException(HttpStatus.BAD_REQUEST,
                "Escolha horários em intervalos de 30 minutos.");
        }
    }
}
