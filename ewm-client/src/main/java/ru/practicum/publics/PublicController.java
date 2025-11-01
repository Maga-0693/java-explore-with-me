package ru.practicum.publics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.coyote.BadRequestException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.practicum.categories.CategoryDto;
import ru.practicum.compilations.CompilationDto;
import ru.practicum.events.EventDto;
import ru.practicum.events.EventShortDto;
import ru.practicum.events.EventSortType;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicController {
    PublicWebClientCategories publicWebClientCategories;
    PublicWebClientCompilations publicWebClientCompilations;
    PublicWebClientEvents publicWebClientEvents;

    @GetMapping("/categories")
    public Mono<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Min(1) Integer size
    ) {
        return publicWebClientCategories.getCategories(from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getCategory(
            @PathVariable @Min(1) Long catId
    ) {
        return publicWebClientCategories.getCategory(catId);
    }

    @GetMapping("/compilations")
    public Mono<List<CompilationDto>> getCompilations(
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Min(1) Integer size
    ) {
        return publicWebClientCompilations.getCompilations(from, size);
    }

    @GetMapping("/compilations/{compId}")
    public CompilationDto getCompilationById(
            @PathVariable @Min(1) Long compId
    ) {
        return publicWebClientCompilations.getCompilationById(compId);
    }

    @GetMapping("/events")
    public Mono<List<EventShortDto>> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventSortType sort,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request

    ) throws BadRequestException {

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("rangeEnd is before rangeStart");
        }

        String clientIp = request.getRemoteAddr();

        return publicWebClientEvents.getEvents(text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                sort,
                from,
                size,
                clientIp);
    }

    @GetMapping("/events/{id}")
    public EventDto getEvent(@PathVariable @Min(1) Long id,
                             HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();

        return publicWebClientEvents.getEvent(id, clientIp);
    }
}
