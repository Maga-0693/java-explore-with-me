package ru.practicum.hit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsInternalController {
    private final StatsService statsService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void addHit(@RequestBody @Valid NewHitRequest request) {
        log.info("Received hit: app={}, uri={}, ip={}, timestamp={}",
                request.getApp(), request.getUri(), request.getIp(), request.getTimestamp());
        statsService.addHit(request);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats(
            @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") boolean unique
    ) {
        // Валидация дат
        if (end.isBefore(start)) {
            return createBadRequestResponse("End time must be after start time");
        }

        log.info("Getting stats for period: {} - {}, uris: {}, unique: {}",
                start, end, uris, unique);

        List<ViewStatsDto> stats = statsService.getStats(start, end, uris, unique);
        return ResponseEntity.ok(stats);
    }

    private ResponseEntity<Map<String, Object>> createBadRequestResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().format(formatter));
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", message);
        errorResponse.put("path", "/stats");

        return ResponseEntity.badRequest().body(errorResponse);
    }
}