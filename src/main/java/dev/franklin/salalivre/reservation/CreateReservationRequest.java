package dev.franklin.salalivre.reservation;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
    @NotNull Long roomId,
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 80) String requester,
    @NotNull LocalDateTime startsAt,
    @NotNull LocalDateTime endsAt
) {
}
