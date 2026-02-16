package isa.jutjub.dto.premiere;

import com.fasterxml.jackson.annotation.JsonFormat;
import isa.jutjub.model.PremiereStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiereSessionDTO {

    private Long id;
    private Long videoId;
    private String videoTitle;
    private PremiereStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    private Double currentPosition;
    private boolean isPlaying;
    private Long viewerCount;
    private boolean allowReplay;
    private boolean chatEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}