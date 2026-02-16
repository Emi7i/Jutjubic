package isa.jutjub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "premiere_sessions")
@Getter
@Setter
public class PremiereSession extends BaseEntity {

    // ---- Core ----

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Videos video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PremiereStatus status = PremiereStatus.SCHEDULED;

    // ---- Scheduling ----

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // ---- Playback State ----

    @Column(name = "current_position_seconds")
    private Double currentPositionSeconds = 0.0;

    @Column(name = "is_playing")
    private boolean playing = false;

    /**
     * Server timestamp (epoch millis) when playback state last changed.
     * Used to compute real-time drift safely.
     */
    @Column(name = "last_state_change_epoch")
    private Long lastStateChangeEpoch;

    // ---- Session Metadata ----

    @Column(name = "viewer_count")
    private Long viewerCount = 0L;

    @Column(name = "allow_replay")
    private boolean allowReplay = false;

    @Column(name = "chat_enabled")
    private boolean chatEnabled = true;

    // ---- Lifecycle Methods ----

    public void startNow() {
        this.status = PremiereStatus.LIVE;
        this.actualStartTime = LocalDateTime.now();
        this.playing = true;
        this.lastStateChangeEpoch = System.currentTimeMillis();
    }

    public void pause(Double position) {
        this.playing = false;
        this.currentPositionSeconds = position;
        this.lastStateChangeEpoch = System.currentTimeMillis();
        this.status = PremiereStatus.PAUSED;
    }

    public void resume() {
        this.playing = true;
        this.lastStateChangeEpoch = System.currentTimeMillis();
        this.status = PremiereStatus.LIVE;
    }

    public void seek(Double position) {
        this.currentPositionSeconds = position;
        this.lastStateChangeEpoch = System.currentTimeMillis();
    }

    public void finish() {
        this.status = PremiereStatus.FINISHED;
        this.endedAt = LocalDateTime.now();
        this.playing = false;
    }

    /**
     * Compute authoritative playback position safely.
     */
    public double computeCurrentPosition() {
        if (!playing || lastStateChangeEpoch == null) {
            return currentPositionSeconds != null ? currentPositionSeconds : 0.0;
        }

        long now = System.currentTimeMillis();
        double delta = (now - lastStateChangeEpoch) / 1000.0;
        return currentPositionSeconds + delta;
    }
}
