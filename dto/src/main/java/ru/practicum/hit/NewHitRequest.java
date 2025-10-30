package ru.practicum.hit;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Value
@Jacksonized
@Builder
public class NewHitRequest {

    @NotBlank(message = "App name must not be blank")
    @Size(max = 255, message = "App name must not exceed 255 characters")
    String app;

    @NotBlank(message = "URI must not be blank")
    @URL(message = "URI must be a valid URL")
    @Size(max = 2048, message = "URI must not exceed 2048 characters")
    String uri;

    @NotBlank(message = "IP address must not be blank")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "Invalid IP address format")
    String ip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "Timestamp must not be null")
    LocalDateTime timestamp;
}