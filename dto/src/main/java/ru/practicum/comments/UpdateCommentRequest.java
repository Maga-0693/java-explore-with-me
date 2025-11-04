package ru.practicum.comments;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UpdateCommentRequest {

    @NotBlank
    String text;
}