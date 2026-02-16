package isa.jutjub.dto.premiere;

import isa.jutjub.model.PremiereStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackStateDTO {

    private Long premiereId;
    private boolean isPlaying;
    private Double currentPosition;

    /**
     * Server timestamp when the state last changed (epoch millis)
     */
    private Long lastStateChangeEpoch;

    /**
     * Current server timestamp when this state was generated (epoch millis)
     * Clients can use this to compute latency and adjust synchronization
     */
    private Long serverTimestamp;

    private PremiereStatus status;
    private Long viewerCount;

    /**
     * Helper method for clients to compute adjusted position
     * accounting for network latency
     */
    public double computeAdjustedPosition(long clientReceiveTime) {
        if (!isPlaying || lastStateChangeEpoch == null) {
            return currentPosition != null ? currentPosition : 0.0;
        }

        // Calculate how much time has passed since state was generated
        double serverDelta = (serverTimestamp - lastStateChangeEpoch) / 1000.0;

        // Calculate latency
        double latency = (clientReceiveTime - serverTimestamp) / 1000.0;

        // Adjust position for both server processing time and network latency
        return currentPosition + latency;
    }
}