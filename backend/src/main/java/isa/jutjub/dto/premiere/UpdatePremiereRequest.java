package isa.jutjub.dto.premiere;

import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePremiereRequest {

    @Future(message = "Scheduled start time must be in the future")
    private LocalDateTime scheduledStartTime;

    private Boolean allowReplay;

    private Boolean chatEnabled;
}