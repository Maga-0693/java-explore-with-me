package ru.practicum.privates;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.practicum.base.BaseWebClient;
import ru.practicum.events.EventDto;
import ru.practicum.events.NewEventRequest;
import ru.practicum.events.UpdateEventRequest;
import ru.practicum.exeption.EventDataException;
import ru.practicum.exeption.ForbiddenException;
import ru.practicum.exeption.NotFoundException;

import java.util.List;

@Service
public class PrivateWebEventsClient extends BaseWebClient {
    private static final String API_PREFIX = "/users";

    public PrivateWebEventsClient(@Value("${ewm-service.url}") String baseUrl) {

        super(baseUrl, API_PREFIX);
    }

    public EventDto addEvent(Long userId, NewEventRequest request) {
        try {

            return webClient.post()
                    .uri(String.format("/%d/events", userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EventDto.class)
                    .block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException(ex.getResponseBodyAsString());
            }
            throw ex;
        }
    }

    public EventDto getEvent(Long userId, Long eventId) {
        try {
            return webClient.get()
                    .uri(String.format("/%d/events/%d", userId, eventId))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(EventDto.class)
                    .block();
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException(ex.getResponseBodyAsString());
            }
            throw ex;
        }

    }

    public Mono<List<EventDto>> getUserEvents(Long userId, Integer from, Integer size) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(String.format("/%d/events", userId))
                        .queryParam("from", from)
                        .queryParam("size", size)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND, response -> {
                    throw new NotFoundException(String.format("User with id=%d was not found", userId));
                })
                .bodyToFlux(EventDto.class)
                .collectList();
    }

    public EventDto updateEvent(Long userId, Long eventId, UpdateEventRequest request) {
        try {
            return webClient.patch()
                    .uri(String.format("/%d/events/%d", userId, eventId))
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EventDto.class)
                    .block();

        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException(ex.getResponseBodyAsString());
            }
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new EventDataException(ex.getResponseBodyAsString());
            } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new ForbiddenException(ex.getResponseBodyAsString());
            }
            throw ex;
        }
    }
}
