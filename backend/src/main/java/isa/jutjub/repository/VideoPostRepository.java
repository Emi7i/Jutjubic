package isa.jutjub.repository;

import isa.jutjub.model.VideoPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface VideoPostRepository extends JpaRepository<VideoPost, Long> {

    /**
     * Find video posts by tags
     * @param tags set of tags to search for
     * @return list of video posts containing any of the specified tags
     */
    @Query("SELECT vp FROM VideoPost vp JOIN vp.tags t WHERE t IN :tags")
    List<VideoPost> findByTags(@Param("tags") Set<String> tags);

    /**
     * Find video posts by title containing keyword
     * @param title keyword to search in title
     * @param pageable pagination information
     * @return page of video posts with title containing keyword
     */
    Page<VideoPost> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Find video posts by location
     * @param location location to search for
     * @param pageable pagination information
     * @return page of video posts from specified location
     */
    Page<VideoPost> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    /**
     * Find video posts created after specified date
     * @param date date to search from
     * @param pageable pagination information
     * @return page of video posts created after specified date
     */
    Page<VideoPost> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);

    /**
     * Find most popular video posts ordered by likes count
     * @param pageable pagination information
     * @return page of most popular video posts
     */
    @Query("SELECT vp FROM VideoPost vp ORDER BY vp.likesCount DESC")
    Page<VideoPost> findMostPopular(Pageable pageable);

    /**
     * Find most recent video posts ordered by creation date
     * @param pageable pagination information
     * @return page of most recent video posts
     */
    @Query("SELECT vp FROM VideoPost vp ORDER BY vp.createdAt DESC")
    Page<VideoPost> findMostRecent(Pageable pageable);

    /**
     * Find video posts by a specific tag
     * @param tag tag to search for
     * @param pageable pagination information
     * @return page of video posts with specified tag
     */
    @Query("SELECT vp FROM VideoPost vp JOIN vp.tags t WHERE t = :tag")
    Page<VideoPost> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * Search video posts by multiple criteria (title, description, tags, location)
     * @param keyword search keyword
     * @param pageable pagination information
     * @return page of video posts matching search criteria
     */
    @Query("SELECT vp FROM VideoPost vp WHERE " +
           "LOWER(vp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(vp.videoDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(vp.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM vp.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<VideoPost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count video posts by user (assuming we add user relationship later)
     * @param userId user ID
     * @return count of video posts by user
     */
    // This will be implemented when we add user relationship
    // Long countByUserId(Long userId);

    /**
     * Find video posts with video file size within range
     * @param minSize minimum file size
     * @param maxSize maximum file size
     * @param pageable pagination information
     * @return page of video posts within file size range
     */
    Page<VideoPost> findByVideoFileSizeBetween(Long minSize, Long maxSize, Pageable pageable);


    // TODO: check if transaction is needed
    /**
     * Atomically increments the view count for a video post
     * This method is thread-safe and handles concurrent access correctly
     * @param id the video post ID
     * @return number of rows updated (should be 1 if successful)
     */
    @Modifying
    @Transactional
    @Query("UPDATE VideoPost vp SET vp.viewsCount = vp.viewsCount + 1 WHERE vp.id = :id")
    int incrementViewsCount(@Param("id") Long id);
}
