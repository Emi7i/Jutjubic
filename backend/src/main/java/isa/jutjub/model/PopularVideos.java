package isa.jutjub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_videos")
@Getter
@Setter
public class PopularVideos extends BaseEntity {

    @Column(name = "run_time", nullable = false)
    private LocalDateTime runTime;

    @Column(name = "video1_id")
    private Long video1Id;

    @Column(name = "video1_score")
    private Double video1Score;

    @Column(name = "video2_id")
    private Long video2Id;

    @Column(name = "video2_score")
    private Double video2Score;

    @Column(name = "video3_id")
    private Long video3Id;

    @Column(name = "video3_score")
    private Double video3Score;

    @PrePersist
    protected void onCreate() {
        runTime = LocalDateTime.now();
    }
}
