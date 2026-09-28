package dev.franklin.salalivre;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import dev.franklin.salalivre.reservation.CreateReservationRequest;
import dev.franklin.salalivre.reservation.ReservationService;

@Configuration
@Profile("demo")
public class DemoDataConfiguration {
    @Bean
    ApplicationRunner sampleReservations(ReservationService service) {
        return args -> {
            LocalDate day = LocalDate.now().plusDays(1);
            if (!service.listForDay(day).isEmpty()) {
                return;
            }
            add(service, day, 1, "Planejamento da equipe", "Marina Costa", "08:00", "09:30");
            add(service, day, 1, "Revisão do projeto", "Carlos Mendes", "14:00", "15:30");
            add(service, day, 2, "Oficina de programação", "Juliana Alves", "09:00", "11:30");
            add(service, day, 2, "Treinamento interno", "Rafael Lima", "13:00", "15:00");
            add(service, day, 3, "Entrevistas", "Fernanda Rocha", "10:00", "11:00");
            add(service, day, 4, "Apresentação mensal", "Bruno Ferreira", "16:00", "18:00");
        };
    }

    private void add(ReservationService service, LocalDate day, long roomId,
                     String title, String requester, String start, String end) {
        service.create(new CreateReservationRequest(
            roomId, title, requester,
            LocalDateTime.of(day, LocalTime.parse(start)),
            LocalDateTime.of(day, LocalTime.parse(end))
        ));
    }
}
