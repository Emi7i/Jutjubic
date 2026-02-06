package isa.jutjub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;            // backend uses Long
    private Long videoId;       // ID of the associated video
    private Long userId;        // optional: can be null for anonymous
    private String userName;    // display name
    private String text;        // comment text
    private LocalDateTime createdAt; // timestamp
}
