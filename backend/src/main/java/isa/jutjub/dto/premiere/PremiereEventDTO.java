package isa.jutjub.dto.premiere;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiereEventDTO {

    private PremiereEventType eventType;
    private Long premiereId;
    private Long timestamp;
    private PremiereSessionDTO data;
    private String message;
}