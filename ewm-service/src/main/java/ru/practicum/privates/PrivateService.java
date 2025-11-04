package ru.practicum.privates;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import ru.practicum.categories.Category;
import ru.practicum.categories.CategoryRepository;
import ru.practicum.comments.*;
import ru.practicum.events.*;
import ru.practicum.exeption.*;
import ru.practicum.requests.*;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrivateService {
    EventRepository eventRepository;
    UserRepository userRepository;
    CategoryRepository categoryRepository;
    RequestsRepository requestsRepository;
    CommentRepository commentRepository;

    EventMapper eventMapper;
    RequestMapper requestMapper;
    CommentMapper commentMapper;

    public EventDto addEvent(Long userId, NewEventRequest request) {
        log.info("Adding event {}", request);
        User initiator = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId))
        );

        Category category = categoryRepository.findById(request.getCategory()).orElseThrow(
                () -> new NotFoundException(String.format("Category with id=%d was not found", request.getCategory()))
        );

        Location location = Location.builder()
                .lat(request.getLocation().getLat())
                .lon(request.getLocation().getLon())
                .build();

        Event event = Event.builder()
                .annotation(request.getAnnotation())
                .category(category)
                .createdOn(LocalDateTime.now())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .location(location)
                .paid(request.getPaid())
                .participantLimit(request.getParticipantLimit())
                .requestModeration(request.getRequestModeration())
                .title(request.getTitle())
                .initiator(initiator)
                .commentDisabled(request.getCommentDisabled())
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Added event {}", savedEvent);
        return eventMapper.toDto(savedEvent);

    }

    @Transactional(readOnly = true)
    public EventDto getEvent(Long userId, Long eventId) {
        log.info("Getting event {}", eventId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );
        log.info("Getting event {}", event);
        return eventMapper.toDto(event);
    }

    @Transactional(readOnly = true)
    public List<EventDto> getUserEvents(Long userId, Integer from, Integer size) {
        log.info("Getting user events from {} to {}", from, size);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> event = eventRepository.findByInitiatorId(userId, pageable);
        return event.stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    public EventDto updateEvent(Long userId, Long eventId, UpdateEventRequest request) {
        log.info("Updating event {}, as request: {}", eventId, request);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(
                    String.format("User with id=%d is not allowed to update event with id=%d", userId, eventId));
        }

        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new EventDataException("Only pending or canceled events can be changed");
        }

        if (request.hasAnnotation()) {
            log.info("Set annotation {}", request.hasAnnotation());
            event.setAnnotation(request.getAnnotation());
        }
        if (request.hasCategory()) {
            event.setCategory(categoryRepository.findById(request.getCategory()).orElseThrow(
                    () -> new NotFoundException(String.format("Category with id=%d was not found", request.getCategory()))
            ));
            log.info("Set category {}", request.getCategory());
        }
        if (request.hasDescription()) {
            log.info("Set event description {}", request.hasDescription());
            event.setDescription(request.getDescription());
        }
        if (request.hasLocation()) {
            Location location = new Location(request.getLocation().getLat(), request.getLocation().getLon());
            log.info("Set location {}", location);
            event.setLocation(location);
        }
        if (request.hasParticipantLimit()) {
            log.info("Set participant limit {}", request.hasParticipantLimit());
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.hasRequestModeration()) {
            log.info("Set request moderation {}", request.hasRequestModeration());
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.hasTitle()) {
            log.info("Set title {}", request.hasTitle());
            event.setTitle(request.getTitle());
        }
        if (request.hasPaid()) {
            log.info("Set paid {}", request.hasPaid());
            event.setPaid(request.getPaid());
        }
        if (request.hasEventDate()) {
            log.info("Set event date {}", request.hasEventDate());
            event.setEventDate(request.getEventDate());
        }

        if (request.hasCommentDisabled()) {
            log.info("Set comment disabled {}", request.hasCommentDisabled());
            event.setCommentDisabled(request.getCommentDisabled());
        }

        if (request.getStateAction() != null) {
            log.info("State action {}", request.getStateAction());
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                default ->
                        throw new EventDataException(String.format("User cannot perform action: %s", request.getStateAction()));
            }
            log.info("State  {}", event.getState());
        }

        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    @Transactional(readOnly = true)
    public List<RequestDto> getRequestsByUserEvent(Long userId, Long eventId) {
        log.info("Get requests for events from {} to user {}", eventId, userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        log.info("Event initiator: {}, userId {}", event.getInitiator().getId(), userId);
        if (!event.getInitiator().getId().equals(userId)) {
            log.info("user: {} not allowed perfom request for event: {}", userId, eventId);
            throw new ForbiddenException(String.format("You are not allowed to perform this action. You are not initiator of event: %d", eventId));
        }

        List<Request> requests = requestsRepository.findByEvent_Id(eventId);
        return requests.stream()
                .map(requestMapper::requestToRequestDto)
                .collect(Collectors.toList());
    }

    public EventRequestStatusUpdateResult updateEventRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest request
    ) {
        log.info("Patch request for requests: {} to status: {}", request.getRequestIds(), request.getStatus());
        log.info("For event: {}", eventId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to perform this action.");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit().equals(0)) {
            throw new ConflictException(String.format(
                    "Request moderation switched off or Participant limit = 0. Event have %d confirmed requests.",
                    event.getConfirmedRequests()));
        }

        if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Participant limit exceeded");
        }

        List<Request> requests = requestsRepository.findByIdIn(request.getRequestIds());

        if (requests.stream().anyMatch(req -> !req.getStatus().equals(RequestStatus.PENDING))) {
            throw new ConflictException("All requests must have status PENDING");
        }

        List<RequestDto> confirmed = new ArrayList<>();
        List<RequestDto> rejected = new ArrayList<>();

        if (request.getStatus().equals(RequestStatus.CONFIRMED)) {
            for (Request req : requests) {
                if (event.getConfirmedRequests() < event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    event.setConfirmedRequests(event.getConfirmedRequests() + 1);
                    confirmed.add(requestMapper.requestToRequestDto(req));
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(requestMapper.requestToRequestDto(req));
                }
            }
        } else {
            requests.forEach(req -> req.setStatus(RequestStatus.REJECTED));
            rejected = requests.stream()
                    .map(requestMapper::requestToRequestDto)
                    .collect(Collectors.toList());
        }

        requestsRepository.saveAll(requests);
        eventRepository.save(event);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }

    public RequestDto addRequest(Long userId, Long eventId) {
        log.info("Adding request from user {}, to event {}", userId, eventId);

        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId)));

        if (requestsRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new DataConflictException(String.format("Event: %d already contain request from user: %d", eventId, userId));
        }

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        if (event.getInitiator().getId().equals(userId)) {
            throw new DataConflictException("Owner of event can not perform request");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new DataConflictException("Event is not published");
        }

        if (event.getParticipantLimit() != 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new DataConflictException("Participant limit exceeded");
        }

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .requester(user)
                .event(event)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit().equals(0)) {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        }

        event = eventRepository.save(event);
        request = requestsRepository.save(request);
        log.info("Request {} added to the event {}", request, event.getId());
        return requestMapper.requestToRequestDto(request);
    }

    @Transactional(readOnly = true)
    public List<RequestDto> getUserRequests(Long userId) {
        log.info("Get requests for events from user {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        List<Request> requests = requestsRepository.findByRequester_Id(userId);
        log.info("Requests found {}", requests.size());
        return requests.stream()
                .map(requestMapper::requestToRequestDto)
                .collect(Collectors.toList());
    }

    public RequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Canceling request from user {}, to request {}", userId, requestId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Request request = requestsRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException(String.format("Request with id=%d was not found", requestId))
        );

        if (!request.getRequester().getId().equals(userId)) {
            throw new ForbiddenException(String.format("User %d cannot perform request %d", userId, request.getRequester().getId()));
        }

        Event event = eventRepository.findById(request.getEvent().getId()).orElseThrow(
                () -> new NotFoundException(String.format("Event for request with id=%d was not found", requestId))
        );

        if (request.getStatus() == RequestStatus.CONFIRMED) {
            log.info("Event {}, had confirmed requests {}",
                    event.getId(),
                    event.getConfirmedRequests());
            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            log.info("After 1 request cancellation event have confirmed requests {}",
                    event.getConfirmedRequests());
            eventRepository.save(event);
        }

        request.setStatus(RequestStatus.CANCELED);

        request = requestsRepository.save(request);
        log.info("request has been cancelled: {} ", request.getId());
        return requestMapper.requestToRequestDto(request);
    }

    public CommentDto addComment(Long userId, Long eventId, NewCommentRequest request) {
        log.info("Adding comment from user {}, to event {}", userId, eventId);

        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId))
        );

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        if (event.getCommentDisabled()) {
            throw new ConflictException(String.format("Comments are disabled for event %d", eventId));
        }

        ru.practicum.comments.Comment comment = ru.practicum.comments.Comment.builder()
                .text(request.getText())
                .author(user)
                .event(event)
                .build();

        return commentMapper.commentToCommentDto(commentRepository.save(comment));

    }

    public CommentDto updateComment(Long userId, Long eventId, Long commentId, UpdateCommentRequest request) {

        checkCommentConditions(userId, eventId);

        ru.practicum.comments.Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException(String.format("Comment with id=%d was not found", commentId))
        );

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(String.format("User %d cannot perform request", userId).trim());
        }

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new ForbiddenException(String.format("Comment with id=%d is not for event=%d", commentId, eventId));
        }

        if (comment.getText().trim().equalsIgnoreCase(request.getText().trim())) {
            throw new ConflictException("Nothing to change");
        }

        comment.setText(request.getText());
        comment.setEdited(true);

        return commentMapper.commentToCommentDto(commentRepository.save(comment));
    }

    public CommentDto updateCommentStatus(Long userId, Long eventId, Long commentId, CommentCommand command) {
        log.info("Deleting comment from user {}, to event {}", userId, eventId);

        checkCommentConditions(userId, eventId);

        ru.practicum.comments.Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException(String.format("Comment with id=%d was not found", commentId)));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(String.format(
                    "User %d cannot perform request. Not Author of comment with id=%d",
                    userId, commentId
            ));
        }

        if (comment.getParentComment() != null) {
            Boolean isParentDeleted = comment.getParentComment().isDeleted();

            if (isParentDeleted && command == CommentCommand.RESTORE) {
                throw new ConflictException(String.format("Comment with id=%d can not be restored. Reason: This comment is reply to deleted comment",
                        commentId
                ).trim());
            }

        }

        switch (command) {
            case DELETE -> {
                markCommentAndRepliesAsDeleted(comment);
                log.info("comment has been marked as deleted: {} ", comment.getId());
            }
            case RESTORE -> {
                markCommentAndRepliesAsRestored(comment);
                log.info("comment has been marked as restored: {} ", comment.getId());
            }
        }
        return commentMapper.commentToCommentDto(commentRepository.save(comment));
    }

    public CommentDto replyToComment(Long userId, Long eventId, Long commentId, NewCommentRequest request) {
        log.info("Replying user {}, event {}, to comment {}", userId, eventId, commentId);
        log.info("Replying request {}", request);

        User author = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId))
        );

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        ru.practicum.comments.Comment parentComment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException(String.format("Parent comment with id=%d was not found", commentId))
        );

        if (event.getCommentDisabled()) {
            throw new ConflictException(String.format("Comments for event with id=%d are disabled", eventId));
        }

        ru.practicum.comments.Comment reply = ru.practicum.comments.Comment.builder()
                .text(request.getText())
                .author(author)
                .event(event)
                .parentComment(parentComment)
                .build();

        reply = commentRepository.save(reply);
        log.info("Reply saved: {} ", reply);

        return commentMapper.commentToCommentDto(reply);
    }

    @Transactional(readOnly = true)
    public Flux<CommentDto> getComments(Long userId, Long eventId, Integer from, Integer size) {
        log.info("Getting comments from user {}, to event {}", userId, eventId);
        checkCommentConditions(userId, eventId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("creationDate").descending());

        Page<ru.practicum.comments.Comment> commentPage = commentRepository.findByEvent_IdAndDeleted(eventId, false, pageable);

        return Flux.fromIterable(commentPage.getContent())
                .map(commentMapper::commentToCommentDto);
    }


    private void markCommentAndRepliesAsDeleted(ru.practicum.comments.Comment comment) {
        comment.setDeleted(true);

        if (!comment.getReplies().isEmpty()) {
            for (ru.practicum.comments.Comment reply : comment.getReplies()) {
                if (!reply.isDeleted()) {
                    reply.setDeleted(true);
                    markCommentAndRepliesAsDeleted(reply);
                }
            }
        }
    }

    private void markCommentAndRepliesAsRestored(ru.practicum.comments.Comment comment) {
        comment.setDeleted(false);

        if (!comment.getReplies().isEmpty()) {
            for (ru.practicum.comments.Comment reply : comment.getReplies()) {
                if (reply.isDeleted()) {
                    reply.setDeleted(false);
                    markCommentAndRepliesAsRestored(reply);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Flux<CommentDto> getUserComments(Long userId, CommentsShowingParam param, Integer from, Integer size) {
        log.info("Getting comments from user {}, to comment {}", userId, param);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("creationDate").descending());

        Page<Comment> comments = switch (param) {
            case SHOW_ALL -> commentRepository.findByAuthor_Id(userId, pageable);
            case SHOW_ACTIVE -> commentRepository
                    .findByAuthor_IdAndDeleted(userId, false, pageable);
            case SHOW_DELETED -> commentRepository
                    .findByAuthor_IdAndDeleted(userId, true, pageable);
        };

        return Flux.fromIterable(comments.getContent())
                .map(commentMapper::commentToCommentDto);

    }

    public SimpleEventDto updateCommentsSetting(Long userId, Long eventId, CommentsSetting command) {
        log.info("Updating comment settings from user {}, to event {}", userId, eventId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(String.format("User with id=%d not allowed to update comment settings", userId));
        }

        if (command == CommentsSetting.DISABLE_COMMENTS && event.getCommentDisabled()) {
            throw new ConflictException("Comments are already disabled");
        }

        if (command == CommentsSetting.ENABLE_COMMENTS && !event.getCommentDisabled()) {
            throw new ConflictException("Comments are already enabled");
        }

        switch (command) {
            case DISABLE_COMMENTS -> event.setCommentDisabled(true);
            case ENABLE_COMMENTS -> event.setCommentDisabled(false);
        }
        log.info("Comments setting updated. Setting {} for event {}", command, eventId);

        return eventMapper.toSimpleDto(eventRepository.save(event));
    }

    private void checkCommentConditions(Long userId, Long eventId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("User with id=%d was not found", userId));
        }
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(String.format("Event with id=%d was not found", eventId));
        }
    }
}