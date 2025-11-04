package ru.practicum.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class NewCommentRequest {

    @NotBlank
    @Size(max = 1000)
    String text;
}