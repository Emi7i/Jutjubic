package isa.jutjub.dto.premiere;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiereChatMessageDTO {
    private Long premiereId;
    private String userId;
    private String username;      // Display name
    private String message;
    private Long timestamp;       // Epoch milliseconds
    private ChatMessageType type; // USER, SYSTEM, ADMIN
}