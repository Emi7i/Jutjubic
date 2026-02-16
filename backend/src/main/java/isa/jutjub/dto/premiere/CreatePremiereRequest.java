package isa.jutjub.dto.premiere;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePremiereRequest {

    @NotNull(message = "Video ID is required")
    private Long videoId;

    @NotNull(message = "Scheduled start time is required")
    @Future(message = "Scheduled start time must be in the future")
    private LocalDateTime scheduledStartTime;

    @Builder.Default
    private boolean allowReplay = false;

    @Builder.Default
    private boolean chatEnabled = true;
}