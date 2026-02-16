package isa.jutjub.service;

import isa.jutjub.dto.premiere.*;
import isa.jutjub.exception.PremiereException;
import isa.jutjub.model.PremiereSession;
import isa.jutjub.model.PremiereStatus;
import isa.jutjub.model.Videos;
import isa.jutjub.repository.PremiereSessionRepository;
import isa.jutjub.repository.VideoPostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PremiereSessionService {

    private final PremiereSessionRepository premiereRepository;
    private final VideoPostRepository videosRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ---- CRUD Operations ----

    public PremiereSession createPremiere(CreatePremiereRequest request) {
        log.info("Creating premiere for video ID: {}", request.getVideoId());

        // Validate video exists
        Videos video = videosRepository.findById(request.getVideoId())
                .orElseThrow(() -> new EntityNotFoundException("Video not found: " + request.getVideoId()));

        // Check for existing active premiere
        if (hasActivePremiere(request.getVideoId())) {
            throw new PremiereException("Video already has an active premiere");
        }

        // Validate scheduled time is in the future
        if (request.getScheduledStartTime().isBefore(LocalDateTime.now())) {
            throw new PremiereException("Scheduled start time must be in the future");
        }

        PremiereSession premiere = new PremiereSession();
        premiere.setVideo(video);
        premiere.setScheduledStartTime(request.getScheduledStartTime());
        premiere.setAllowReplay(request.isAllowReplay());
        premiere.setChatEnabled(request.isChatEnabled());
        premiere.setStatus(PremiereStatus.SCHEDULED);

        PremiereSession saved = premiereRepository.save(premiere);
        log.info("Premiere created with ID: {}", saved.getId());

        // Broadcast premiere created event
        broadcastEvent(saved.getId(), PremiereEventType.CREATED, toDTO(saved));

        return saved;
    }


    @Transactional(readOnly = true)
    public PremiereSession getPremiereById(Long id) {
        return premiereRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Premiere not found: " + id));
    }


    @Transactional(readOnly = true)
    public PremiereSessionDTO getPremiereDTO(Long id) {
        return toDTO(getPremiereById(id));
    }

    public PremiereSession updatePremiere(Long id, UpdatePremiereRequest request) {
        log.info("Updating premiere ID: {}", id);

        PremiereSession premiere = getPremiereById(id);

        // Only allow updates if not finished or cancelled
        if (premiere.getStatus() == PremiereStatus.FINISHED ||
                premiere.getStatus() == PremiereStatus.CANCELLED) {
            throw new PremiereException("Cannot update finished or cancelled premiere");
        }

        if (request.getScheduledStartTime() != null) {
            if (premiere.getStatus() != PremiereStatus.SCHEDULED) {
                throw new PremiereException("Cannot change start time of active premiere");
            }
            premiere.setScheduledStartTime(request.getScheduledStartTime());
        }

        if (request.getAllowReplay() != null) {
            premiere.setAllowReplay(request.getAllowReplay());
        }

        if (request.getChatEnabled() != null) {
            premiere.setChatEnabled(request.getChatEnabled());
        }

        PremiereSession updated = premiereRepository.save(premiere);
        broadcastEvent(id, PremiereEventType.UPDATED, toDTO(updated));

        return updated;
    }

    public void cancelPremiere(Long id) {
        log.info("Cancelling premiere ID: {}", id);

        PremiereSession premiere = getPremiereById(id);

        if (premiere.getStatus() == PremiereStatus.FINISHED) {
            throw new PremiereException("Cannot cancel finished premiere");
        }

        premiere.setStatus(PremiereStatus.CANCELLED);
        premiere.setEndedAt(LocalDateTime.now());
        premiereRepository.save(premiere);

        broadcastEvent(id, PremiereEventType.CANCELLED, null);
        log.info("Premiere cancelled: {}", id);
    }

    // ---- Playback Control ----

    public PremiereSession startPremiere(Long id) {
        log.info("Starting premiere ID: {}", id);

        PremiereSession premiere = getPremiereById(id);
        validatePremiereState(id, PremiereStatus.SCHEDULED);

        premiere.startNow();
        PremiereSession started = premiereRepository.save(premiere);

        broadcastEvent(id, PremiereEventType.STARTED, toDTO(started));

        PlaybackStateDTO state = getPlaybackState(id);
        broadcastPlaybackState(id, state);

        log.info("Premiere started: {}", id);
        return started;
    }


    public PremiereSession pausePremiere(Long id, Double position) {
        log.info("Pausing premiere ID: {} at position: {}", id, position);

        PremiereSession premiere = getPremiereById(id);
        validatePremiereState(id, PremiereStatus.LIVE);

        if (position == null) {
            position = premiere.computeCurrentPosition();
        }

        premiere.pause(position);
        PremiereSession paused = premiereRepository.save(premiere);

        PlaybackStateDTO state = getPlaybackState(id);
        broadcastPlaybackState(id, state);

        broadcastEvent(id, PremiereEventType.PAUSED, toDTO(paused));
        return paused;
    }

    public PremiereSession resumePremiere(Long id) {
        log.info("Resuming premiere ID: {}", id);

        PremiereSession premiere = getPremiereById(id);
        validatePremiereState(id, PremiereStatus.PAUSED);

        premiere.resume();
        PremiereSession resumed = premiereRepository.save(premiere);

        PlaybackStateDTO state = getPlaybackState(id);
        broadcastPlaybackState(id, state);
        broadcastEvent(id, PremiereEventType.RESUMED, toDTO(resumed));

        return resumed;
    }

    public PremiereSession seekPremiere(Long id, Double position) {
        log.info("Seeking premiere ID: {} to position: {}", id, position);

        PremiereSession premiere = getPremiereById(id);
        validatePremiereState(id, PremiereStatus.LIVE, PremiereStatus.PAUSED);

        if (position < 0) {
            throw new PremiereException("Position cannot be negative");
        }

        premiere.seek(position);
        PremiereSession seeked = premiereRepository.save(premiere);

        PlaybackStateDTO state = getPlaybackState(id);
        broadcastPlaybackState(id, state);
        broadcastEvent(id, PremiereEventType.SEEKED, toDTO(seeked));
        return seeked;
    }

    public PremiereSession finishPremiere(Long id) {
        log.info("Finishing premiere ID: {}", id);

        PremiereSession premiere = getPremiereById(id);
        validatePremiereState(id, PremiereStatus.LIVE, PremiereStatus.PAUSED);

        premiere.finish();
        PremiereSession finished = premiereRepository.save(premiere);

        broadcastEvent(id, PremiereEventType.FINISHED, toDTO(finished));
        log.info("Premiere finished: {}", id);

        return finished;
    }


    @Transactional(readOnly = true)
    public PlaybackStateDTO getPlaybackState(Long id) {
        PremiereSession premiere = getPremiereById(id);

        return PlaybackStateDTO.builder()
                .premiereId(id)
                .isPlaying(premiere.isPlaying())
                .currentPosition(premiere.computeCurrentPosition())
                .lastStateChangeEpoch(premiere.getLastStateChangeEpoch())
                .serverTimestamp(System.currentTimeMillis())
                .status(premiere.getStatus())
                .viewerCount(premiere.getViewerCount())
                .build();
    }

    // ---- Viewer Management ----

    public void joinPremiere(Long premiereId, Long userId) {
        log.debug("User {} joining premiere {}", userId, premiereId);

        PremiereSession premiere = getPremiereById(premiereId);

        // Allow SCHEDULED for waiting room
        if (premiere.getStatus() != PremiereStatus.LIVE &&
                premiere.getStatus() != PremiereStatus.PAUSED &&
                premiere.getStatus() != PremiereStatus.SCHEDULED) {
            throw new PremiereException("Premiere is not available");
        }

        premiereRepository.incrementViewerCount(premiereId);

        // Broadcast updated viewer count
        PlaybackStateDTO state = getPlaybackState(premiereId);
        broadcastPlaybackState(premiereId, state);


        if (premiere.isChatEnabled()) {
            String username = extractUsername(userId);
            broadcastSystemMessage(premiereId, username + " joined the premiere");
        }

        log.debug("User {} joined premiere {}. Current viewers: {}",
                userId, premiereId, state.getViewerCount());
    }


    public void leavePremiere(Long premiereId, String userId) {
        log.debug("User {} leaving premiere {}", userId, premiereId);

        premiereRepository.decrementViewerCount(premiereId);

        // Broadcast updated viewer count
        try {
            PlaybackStateDTO state = getPlaybackState(premiereId);
            broadcastPlaybackState(premiereId, state);
            log.debug("User {} left premiere {}. Current viewers: {}",
                    userId, premiereId, state.getViewerCount());
        } catch (Exception e) {
            log.warn("Could not broadcast viewer count after user {} left: {}", userId, e.getMessage());
        }
        try {
            PremiereSession premiere = getPremiereById(premiereId);
            if (premiere.isChatEnabled()) {
                String username = extractUsername(Long.getLong(userId));
                broadcastSystemMessage(premiereId, username + " left the premiere");
            }
        } catch (Exception e) {
            log.warn("Could not send leave message", e);
        }
    }


    @Transactional(readOnly = true)
    public Long getViewerCount(Long premiereId) {
        PremiereSession premiere = getPremiereById(premiereId);
        return premiere.getViewerCount();
    }

    // ---- Query Operations ----

    @Transactional(readOnly = true)
    public List<PremiereSession> getPremieresByVideo(Long videoId) {
        return premiereRepository.findByVideo_Id(videoId);
    }

    @Transactional(readOnly = true)
    public Page<PremiereSessionDTO> getPremieresByStatus(PremiereStatus status, Pageable pageable) {
        return premiereRepository.findByStatus(status, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PremiereSessionDTO> getUpcomingPremieres(Pageable pageable) {
        return premiereRepository.findByStatusOrderByScheduledStartTimeAsc(
                        PremiereStatus.SCHEDULED, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<PremiereSessionDTO> getLivePremieres() {
        return premiereRepository.findByStatus(PremiereStatus.LIVE).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PremiereSessionDTO> searchPremieres(String keyword, Pageable pageable) {
        return premiereRepository.searchByVideoTitle(keyword, pageable)
                .map(this::toDTO);
    }

    // ---- Scheduled Tasks ----

    public void autoStartScheduledPremieres() {
        List<PremiereSession> premieres = premiereRepository
                .findByStatusAndScheduledStartTimeBefore(
                        PremiereStatus.SCHEDULED,
                        LocalDateTime.now()
                );

        for (PremiereSession premiere : premieres) {
            try {
                log.info("Auto-starting scheduled premiere: {}", premiere.getId());
                startPremiere(premiere.getId());
            } catch (Exception e) {
                log.error("Failed to auto-start premiere {}: {}", premiere.getId(), e.getMessage());
            }
        }
    }


    public void autoFinishLivePremieres() {
        List<PremiereSession> live = premiereRepository.findByStatus(PremiereStatus.LIVE);

        for (PremiereSession premiere : live) {

            double position = premiere.computeCurrentPosition();
            Videos video = premiere.getVideo();
            double duration = premiere.getVideo().getVideoDuration();

            if (position >= duration) {
                finishPremiere(premiere.getId());
            }
        }
    }


    public void cleanupOldPremieres(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<PremiereSession> oldPremieres = premiereRepository
                .findByStatusAndEndedAtBefore(PremiereStatus.FINISHED, cutoffDate);

        log.info("Cleaning up {} old premieres", oldPremieres.size());
        // Additional cleanup logic here (e.g., archiving, soft delete)
    }

    // ---- Validation ----

    @Transactional(readOnly = true)
    public boolean hasActivePremiere(Long videoId) {
        return premiereRepository.findByVideo_IdAndStatus(videoId, PremiereStatus.SCHEDULED).isPresent()
                || premiereRepository.findByVideo_IdAndStatus(videoId, PremiereStatus.LIVE).isPresent()
                || premiereRepository.findByVideo_IdAndStatus(videoId, PremiereStatus.PAUSED).isPresent();
    }

    public boolean canControlPremiere(Long premiereId, String userId) {
        // Implementation depends on your user/authorization model
        // Check if user is admin or video owner
        PremiereSession premiere = getPremiereById(premiereId);
        // return premiere.getVideo().getUser().getId().equals(userId) || isAdmin(userId);
        return true; // Placeholder
    }

    public void validatePremiereState(Long id, PremiereStatus... allowedStatuses) {
        PremiereSession premiere = getPremiereById(id);
        boolean valid = Arrays.asList(allowedStatuses).contains(premiere.getStatus());

        if (!valid) {
            throw new PremiereException(
                    String.format("Premiere %d is in state %s, but action requires one of: %s",
                            id, premiere.getStatus(), Arrays.toString(allowedStatuses))
            );
        }
    }

    // ---- WebSocket Broadcasting ----

    private void broadcastPlaybackState(Long premiereId, PlaybackStateDTO state) {
        String destination = String.format("/topic/premiere/%d/playback", premiereId);
        messagingTemplate.convertAndSend(destination, state);
        log.debug("Broadcasted playback state to: {}", destination);
    }

    private void broadcastEvent(Long premiereId, PremiereEventType eventType, PremiereSessionDTO data) {
        String destination = String.format("/topic/premiere/%d/events", premiereId);
        PremiereEventDTO event = PremiereEventDTO.builder()
                .eventType(eventType)
                .premiereId(premiereId)
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Broadcasted event {} to: {}", eventType, destination);
    }

    /**
     * Broadcast chat message to all premiere viewers
     */
    public void broadcastChatMessage(Long premiereId, String userId, String username, String message) {
        log.debug("💬 Broadcasting chat message to premiere {}", premiereId);

        // Create chat message DTO
        PremiereChatMessageDTO chatMessage = PremiereChatMessageDTO.builder()
                .premiereId(premiereId)
                .userId(userId)
                .username(username)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .type(ChatMessageType.USER)
                .build();

        // Broadcast to chat topic
        String destination = String.format("/topic/premiere/%d/chat", premiereId);
        messagingTemplate.convertAndSend(destination, chatMessage);

        log.debug("📤 Sent chat message to {}", destination);
    }

    /**
     * Broadcast system message (user joined/left)
     */
    public void broadcastSystemMessage(Long premiereId, String message) {
        log.debug("📢 Broadcasting system message to premiere {}", premiereId);

        PremiereChatMessageDTO systemMessage = PremiereChatMessageDTO.builder()
                .premiereId(premiereId)
                .userId("system")
                .username("System")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .type(ChatMessageType.SYSTEM)
                .build();

        String destination = String.format("/topic/premiere/%d/chat", premiereId);
        messagingTemplate.convertAndSend(destination, systemMessage);
    }

    // ---- DTO Conversion ----

    private PremiereSessionDTO toDTO(PremiereSession premiere) {
        return PremiereSessionDTO.builder()
                .id(premiere.getId())
                .videoId(premiere.getVideo().getId())
                .videoTitle(premiere.getVideo().getTitle())
                .status(premiere.getStatus())
                .scheduledStartTime(premiere.getScheduledStartTime())
                .actualStartTime(premiere.getActualStartTime())
                .endedAt(premiere.getEndedAt())
                .currentPosition(premiere.computeCurrentPosition())
                .isPlaying(premiere.isPlaying())
                .viewerCount(premiere.getViewerCount())
                .allowReplay(premiere.isAllowReplay())
                .chatEnabled(premiere.isChatEnabled())
                .createdAt(premiere.getCreatedAt())
                .updatedAt(premiere.getUpdatedAt())
                .build();
    }

    private String extractUsername(long userId){
        return "Anonymous";
    }
}