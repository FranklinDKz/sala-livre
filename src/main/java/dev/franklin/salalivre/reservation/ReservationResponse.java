package dev.franklin.salalivre.reservation;

import java.time.LocalDateTime;

public record ReservationResponse(
    Long id,
    Long roomId,
    String roomName,
    String title,
    String requester,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    ReservationStatus status,
    LocalDateTime createdAt,
    LocalDateTime cancelledAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(), reservation.getRoom().getId(), reservation.getRoom().getName(),
            reservation.getTitle(), reservation.getRequester(), reservation.getStartsAt(),
            reservation.getEndsAt(), reservation.getStatus(), reservation.getCreatedAt(),
            reservation.getCancelledAt()
        );
    }
}
