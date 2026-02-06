package isa.jutjub.controller;

import isa.jutjub.dto.CommentDto;
import isa.jutjub.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/video-posts/{videoId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long videoId) {
        return ResponseEntity.ok(commentService.getCommentsForVideo(videoId));
    }

    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long videoId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userName,
            @RequestBody CommentDto commentDto
    ) {
        CommentDto created = commentService.addComment(videoId, userId, userName, commentDto.getText());
        return ResponseEntity.ok(created);
    }
}
