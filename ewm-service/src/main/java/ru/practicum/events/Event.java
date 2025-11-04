package ru.practicum.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.categories.Category;
import ru.practicum.compilations.Compilation;
import ru.practicum.user.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Table(name = "events")
@Builder
@EqualsAndHashCode
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String annotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(nullable = false)
    String description;

    @Column(name = "event_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    @Embedded
    Location location;

    @Column(nullable = false)
    @Builder.Default
    Boolean paid = false;

    @Column(name = "participant_limit", nullable = false)
    @Builder.Default
    Integer participantLimit = 0;

    @Column(name = "request_moderation", nullable = false)
    @Builder.Default
    Boolean requestModeration = true;

    @Column(nullable = false)
    String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    User initiator;

    @Column(name = "created_on", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdOn;

    @Column(name = "published_on")  // ← nullable = true (по умолчанию)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime publishedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    EventState state = EventState.PENDING;

    @Column(name = "confirmed_requests", nullable = false)
    @Builder.Default
    Integer confirmedRequests = 0;

    @Builder.Default
    Long views = 0L;

    @ManyToMany(mappedBy = "events", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    Set<Compilation> compilations = new HashSet<>();

    @Column(name = "comments_disabled")
    @Builder.Default
    Boolean commentDisabled = false;
}