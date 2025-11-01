package ru.practicum.exeption;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ErrorResponse {
    String timestamp;
    Integer status;
    String error;
    String message;
    String path;
}
