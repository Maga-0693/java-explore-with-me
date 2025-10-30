package ru.practicum.hit;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsInternalController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void addHit(@RequestBody @Valid NewHitRequest request) {
        log.info("Received hit: app={}, uri={}, ip={}, timestamp={}",
                request.getApp(), request.getUri(), request.getIp(), request.getTimestamp());
        statsService.addHit(request);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique
    ) {
        try {
            validateTimeRange(start, end);

            log.info("Getting stats for period: {} - {}, uris: {}, unique: {}",
                    start, end, uris, unique);

            List<ViewStatsDto> stats = statsService.getStats(start, end, uris, unique);
            return ResponseEntity.ok(stats);

        } catch (IllegalArgumentException ex) {
            log.warn("Bad request for stats: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(ex.getMessage()));
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new IllegalArgumentException("Start time must not be null");
        }

        if (end == null) {
            throw new IllegalArgumentException("End time must not be null");
        }

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    private ErrorResponse createErrorResponse(String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .build();
    }
}