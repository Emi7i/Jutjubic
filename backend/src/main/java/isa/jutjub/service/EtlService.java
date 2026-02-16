package isa.jutjub.service;

import isa.jutjub.model.PopularVideos;
import isa.jutjub.repository.PopularVideosRepository;
import isa.jutjub.repository.ViewEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class EtlService {

    private final ViewEventRepository viewEventRepository;
    private final PopularVideosRepository popularVideosRepository;

    @Autowired
    public EtlService(ViewEventRepository viewEventRepository, PopularVideosRepository popularVideosRepository) {
        this.viewEventRepository = viewEventRepository;
        this.popularVideosRepository = popularVideosRepository;
    }

    /**
     * Run the ETL pipeline
     */
    @Transactional
    @Scheduled(cron = "0 */3 * * * ?")
    public void runEtl() {
        log.info("Starting ETL pipeline");

        // Extract
        List<Object[]> dailyCounts = extract();

        // Transform
        // Map <videoId, score>
        Map<Long, Double> scores = transform(dailyCounts);

        // Load
        load(scores);
    }

    /**
     * Extract: Get daily view counts from last 7 days
     */
    private List<Object[]> extract() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> dailyCounts = viewEventRepository.getDailyViewCounts(sevenDaysAgo);
        return dailyCounts;
    }

    /**
     * Transform: Calculate popularity scores for each video
     */
    private Map<Long, Double> transform(List<Object[]> dailyCounts) {
        Map<Long, Map<LocalDate, Long>> videoDailyViews = new HashMap<>();

        // Group by video and date
        for (Object[] row : dailyCounts) {
            Long videoId = (Long) row[0];
            LocalDate date = ((java.sql.Date) row[1]).toLocalDate();
            Long count = (Long) row[2];

            videoDailyViews.computeIfAbsent(videoId, k -> new HashMap<>()).put(date, count);
        }

        // Calculate scores
        Map<Long, Double> scores = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (Map.Entry<Long, Map<LocalDate, Long>> entry : videoDailyViews.entrySet()) {
            Long videoId = entry.getKey();
            Map<LocalDate, Long> dailyViews = entry.getValue();

            double score = 0.0;
            for (Map.Entry<LocalDate, Long> dayEntry : dailyViews.entrySet()) {
                LocalDate date = dayEntry.getKey();
                long daysAgo = ChronoUnit.DAYS.between(date, today);
                if (daysAgo <= 7) {
                    int weight = 8 - (int) daysAgo; // 8 for today, 7 for yesterday, ..., 1 for 7 days ago
                    score += dayEntry.getValue() * weight;
                }
            }
            scores.put(videoId, score);
        }

        log.info("Transformed scores for {} videos", scores.size());
        return scores;
    }

    /**
     * Load: Save top 3 popular videos to database
     */
    private void load(Map<Long, Double> scores) {
        // Sort by score descending
        List<Map.Entry<Long, Double>> sortedScores = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(3)
                .toList();

        PopularVideos popularVideos = new PopularVideos();
        if (sortedScores.size() > 0) {
            popularVideos.setVideo1Id(sortedScores.get(0).getKey());
            popularVideos.setVideo1Score(sortedScores.get(0).getValue());
        }
        if (sortedScores.size() > 1) {
            popularVideos.setVideo2Id(sortedScores.get(1).getKey());
            popularVideos.setVideo2Score(sortedScores.get(1).getValue());
        }
        if (sortedScores.size() > 2) {
            popularVideos.setVideo3Id(sortedScores.get(2).getKey());
            popularVideos.setVideo3Score(sortedScores.get(2).getValue());
        }

        popularVideosRepository.save(popularVideos);
        log.info("Loaded top 3 popular videos");
    }
}
