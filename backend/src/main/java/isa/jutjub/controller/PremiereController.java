package isa.jutjub.controller;

import isa.jutjub.dto.premiere.*;
import isa.jutjub.model.PremiereStatus;
import isa.jutjub.service.PremiereSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/premieres")
@RequiredArgsConstructor
public class PremiereController {

    private final PremiereSessionService premiereService;

    // ---- CRUD Operations ----

    @PostMapping
    public ResponseEntity<PremiereSessionDTO> createPremiere(
            @Valid @RequestBody CreatePremiereRequest request) {
        var premiere = premiereService.createPremiere(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(premiereService.getPremiereDTO(premiere.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PremiereSessionDTO> getPremiere(@PathVariable Long id) {
        return ResponseEntity.ok(premiereService.getPremiereDTO(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PremiereSessionDTO> updatePremiere(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePremiereRequest request) {
        var premiere = premiereService.updatePremiere(id, request);
        return ResponseEntity.ok(premiereService.getPremiereDTO(premiere.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPremiere(@PathVariable Long id) {
        premiereService.cancelPremiere(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Playback Control ----

    @PostMapping("/{id}/start")
    public ResponseEntity<PlaybackStateDTO> startPremiere(@PathVariable Long id) {
        premiereService.startPremiere(id);
        return ResponseEntity.ok(premiereService.getPlaybackState(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<PlaybackStateDTO> pausePremiere(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Double> body) {
        Double position = body != null ? body.get("position") : null;
        premiereService.pausePremiere(id, position);
        return ResponseEntity.ok(premiereService.getPlaybackState(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<PlaybackStateDTO> resumePremiere(@PathVariable Long id) {
        premiereService.resumePremiere(id);
        return ResponseEntity.ok(premiereService.getPlaybackState(id));
    }

    @PostMapping("/{id}/seek")
    public ResponseEntity<PlaybackStateDTO> seekPremiere(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body) {
        Double position = body.get("position");
        if (position == null) {
            return ResponseEntity.badRequest().build();
        }
        premiereService.seekPremiere(id, position);
        return ResponseEntity.ok(premiereService.getPlaybackState(id));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<PremiereSessionDTO> finishPremiere(@PathVariable Long id) {
        var premiere = premiereService.finishPremiere(id);
        return ResponseEntity.ok(premiereService.getPremiereDTO(premiere.getId()));
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<PlaybackStateDTO> getPlaybackState(@PathVariable Long id) {
        return ResponseEntity.ok(premiereService.getPlaybackState(id));
    }

    // ---- Query Operations ----

    @GetMapping
    public ResponseEntity<Page<PremiereSessionDTO>> getPremieres(
            @RequestParam(required = false) PremiereStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PremiereSessionDTO> result;

        if (status != null) {
            result = premiereService.getPremieresByStatus(status, pageable);
        } else if (search != null && !search.isBlank()) {
            result = premiereService.searchPremieres(search, pageable);
        } else {
            result = premiereService.getPremieresByStatus(null, pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<PremiereSessionDTO>> getUpcomingPremieres(
            @PageableDefault(size = 20, sort = "scheduledStartTime") Pageable pageable) {
        return ResponseEntity.ok(premiereService.getUpcomingPremieres(pageable));
    }

    @GetMapping("/live")
    public ResponseEntity<List<PremiereSessionDTO>> getLivePremieres() {
        return ResponseEntity.ok(premiereService.getLivePremieres());
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<PremiereSessionDTO>> getPremieresByVideo(
            @PathVariable Long videoId) {
        var premieres = premiereService.getPremieresByVideo(videoId);
        var dtos = premieres.stream()
                .map(p -> premiereService.getPremiereDTO(p.getId()))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}/viewer-count")
    public ResponseEntity<Map<String, Long>> getViewerCount(@PathVariable Long id) {
        Long count = premiereService.getViewerCount(id);
        return ResponseEntity.ok(Map.of("viewerCount", count));
    }

    // ---- Health Check ----

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        var livePremieres = premiereService.getLivePremieres();
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "livePremiereCount", livePremieres.size()
        ));
    }
}