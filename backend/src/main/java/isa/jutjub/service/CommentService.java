package isa.jutjub.service;

import isa.jutjub.dto.CommentDto;
import isa.jutjub.model.Comment;
import isa.jutjub.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentDto addComment(Long videoId, Long userId, String userName, String text) {
        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setUserName(userName != null ? userName : "Anonymous");
        comment.setText(text);

        Comment saved = commentRepository.save(comment);
        return mapToDto(saved);
    }

    public List<CommentDto> getCommentsForVideo(Long videoId) {
        return commentRepository.findAllByVideoIdOrderByCreatedAtAsc(videoId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CommentDto mapToDto(Comment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getVideoId(),
                comment.getUserId(),
                comment.getUserName(),
                comment.getText(),
                comment.getCreatedAt()
        );
    }
}
