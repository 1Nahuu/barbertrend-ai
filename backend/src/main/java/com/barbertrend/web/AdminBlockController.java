package com.barbertrend.web;

import com.barbertrend.dto.CreateScheduleBlockRequest;
import com.barbertrend.dto.ScheduleBlockResponse;
import com.barbertrend.service.ScheduleBlockService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API privada para cerrar la agenda.
 *
 * <p>Los endpoints viven bajo {@code /api/admin/**}, así que
 * {@code SecurityConfig} ya exige sesión y CSRF. El controlador solo traduce
 * HTTP; las reglas están en {@link ScheduleBlockService}.
 */
@RestController
@RequestMapping("/api/admin/blocks")
public class AdminBlockController {

    private final ScheduleBlockService scheduleBlockService;

    public AdminBlockController(ScheduleBlockService scheduleBlockService) {
        this.scheduleBlockService = scheduleBlockService;
    }

    /** Cierres vigentes o futuros. */
    @GetMapping
    public List<ScheduleBlockResponse> list() {
        return scheduleBlockService.listUpcoming().stream()
                .map(ScheduleBlockResponse::from)
                .toList();
    }

    /** Crea un cierre de día completo o de una franja horaria. */
    @PostMapping
    public ResponseEntity<ScheduleBlockResponse> create(
            @Valid @RequestBody CreateScheduleBlockRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ScheduleBlockResponse.from(scheduleBlockService.create(request)));
    }

    /** Quita un cierre. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleBlockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
