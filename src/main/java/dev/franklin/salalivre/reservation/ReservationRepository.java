package dev.franklin.salalivre.reservation;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
        select count(reservation) from Reservation reservation
        where reservation.room.id = :roomId
          and reservation.status = dev.franklin.salalivre.reservation.ReservationStatus.ACTIVE
          and reservation.startsAt < :endsAt
          and reservation.endsAt > :startsAt
        """)
    long countConflicts(@Param("roomId") Long roomId,
                        @Param("startsAt") LocalDateTime startsAt,
                        @Param("endsAt") LocalDateTime endsAt);

    @Query("""
        select reservation from Reservation reservation join fetch reservation.room
        where reservation.startsAt >= :start and reservation.startsAt < :end
        order by reservation.startsAt, reservation.room.name
        """)
    List<Reservation> findForDay(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}
