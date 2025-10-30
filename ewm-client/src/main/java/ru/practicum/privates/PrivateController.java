package ru.practicum.privates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.practicum.events.EventDto;
import ru.practicum.events.NewEventRequest;
import ru.practicum.events.UpdateEventRequest;
import ru.practicum.requests.EventRequestStatusUpdateRequest;
import ru.practicum.requests.EventRequestStatusUpdateResult;
import ru.practicum.requests.RequestDto;
import ru.practicum.util.RequestsValidator;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrivateController {
    PrivateWebEventsClient privateWebEventsClient;
    PrivateWebRequestsClient privateWebRequestsClient;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto addEvent(
            @PathVariable("userId") @Min(1) Long userId,
            @RequestBody @Valid NewEventRequest request) throws BadRequestException {

        RequestsValidator.dateValidation(request);

        return privateWebEventsClient.addEvent(userId, request);
    }

    @GetMapping("/events/{eventId}")
    public EventDto getEvent(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId
    ) {
        return privateWebEventsClient.getEvent(userId, eventId);
    }

    @GetMapping("/events")
    public Mono<List<EventDto>> getEvents(
            @PathVariable @Min(1) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Min(1) Integer size
    ) {
        return privateWebEventsClient.getUserEvents(userId, from, size);
    }

    @PatchMapping("/events/{eventId}")
    public EventDto updateEvent(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid UpdateEventRequest request
    ) throws BadRequestException {

        RequestsValidator.dateValidation(request);

        return privateWebEventsClient.updateEvent(userId, eventId, request);
    }

    @GetMapping("/events/{eventId}/requests")
    public Mono<List<RequestDto>> getRequests(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId
    ) {
        return privateWebRequestsClient.getRequestsByUserEvent(userId, eventId);
    }

    @PatchMapping("events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId,
            @RequestBody @Valid EventRequestStatusUpdateRequest request
    ) {
        return privateWebRequestsClient.updateEventRequestsStatus(userId, eventId, request);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto addRequest(
            @PathVariable @Min(1) Long userId,
            @RequestParam @Min(1) Long eventId
    ) {
        return privateWebRequestsClient.addRequest(userId, eventId);
    }

    @GetMapping("/requests")
    public Mono<List<RequestDto>> getUserRequests(
            @PathVariable @Min(1) Long userId
    ) {
        return privateWebRequestsClient.getUserRequests(userId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public RequestDto cancelRequest(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long requestId
    ) {
        return privateWebRequestsClient.cancelRequest(userId, requestId);
    }
}
