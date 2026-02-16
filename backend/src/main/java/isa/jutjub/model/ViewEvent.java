package isa.jutjub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_events")
@Getter
@Setter
public class ViewEvent extends BaseEntity {

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}
