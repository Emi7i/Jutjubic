package isa.jutjub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import isa.jutjub.config.RabbitMQConfig;
import isa.jutjub.dto.VideoUploadEventDTO;
import isa.jutjub.model.Videos;
import isa.jutjub.proto.VideoEventProtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.address:localhost}")
    private String serverAddress;

    /**
     * Publish video upload event to both JSON and Protobuf queues
     */
    public void publishVideoUploadEvent(Videos video) {
        log.info("📤 Publishing video upload event for video ID: {}", video.getId());

        // Measure serialization times
        long jsonStartTime = System.nanoTime();
        int jsonSize = publishAsJson(video);
        long jsonEndTime = System.nanoTime();
        long jsonSerializationTime = (jsonEndTime - jsonStartTime) / 1000; // microseconds

        long protobufStartTime = System.nanoTime();
        int protobufSize = publishAsProtobuf(video);
        long protobufEndTime = System.nanoTime();
        long protobufSerializationTime = (protobufEndTime - protobufStartTime) / 1000; // microseconds

        log.info("✅ Event published to both queues");
        log.info("   JSON: {} bytes, {} μs", jsonSize, jsonSerializationTime);
        log.info("   Protobuf: {} bytes, {} μs", protobufSize, protobufSerializationTime);

        if (protobufSerializationTime < jsonSerializationTime) {
            double speedup = (double) jsonSerializationTime / protobufSerializationTime;
            log.info("   ⚡ Protobuf is {:.2f}x faster", speedup);
        }

        double sizeReduction = ((double)(jsonSize - protobufSize) / jsonSize) * 100;
        log.info("   📦 Protobuf is {:.2f}% smaller", sizeReduction);
    }

    private int publishAsJson(Videos video) {
        try {
            VideoUploadEventDTO event = createEventDTO(video);

            byte[] messageBytes = objectMapper.writeValueAsBytes(event);

            rabbitTemplate.send(
                    "",
                    RabbitMQConfig.JSON_QUEUE,
                    new org.springframework.amqp.core.Message(
                            messageBytes,
                            new org.springframework.amqp.core.MessageProperties()
                    )
            );

            return messageBytes.length;

        } catch (Exception e) {
            log.error("Error publishing JSON message", e);
            return 0;
        }
    }


    private int publishAsProtobuf(Videos video) {
        try {
            isa.jutjub.proto.VideoUploadEvent.Builder builder =
                    isa.jutjub.proto.VideoUploadEvent.newBuilder()
                            .setVideoId(video.getId())
                            .setTitle(video.getTitle())
                            .setDescription(video.getVideoDescription() != null ? video.getVideoDescription() : "")
                            .setFileSizeBytes(video.getVideoFileSize() != null ? video.getVideoFileSize() : 0)
                            .setDurationSeconds(video.getVideoDuration() != null ? video.getVideoDuration() : 0.0)
                            .setFormat("mp4")
                            .setUploadTimestamp(System.currentTimeMillis())
                            .setVideoPath(video.getVideoPath() != null ? video.getVideoPath() : "")
                            .setThumbnailPath(video.getThumbnailPath() != null ? video.getThumbnailPath() : "")
                            .setVideoUrl(buildVideoUrl(video.getId()))
                            .setThumbnailUrl(buildThumbnailUrl(video.getId()))
                            .setViewsCount(video.getViewsCount() != null ? video.getViewsCount() : 0L)
                            .setLikesCount(video.getLikesCount() != null ? video.getLikesCount() : 0L)
                            .setCommentsCount(video.getCommentsCount() != null ? video.getCommentsCount() : 0L);

            // Add tags
            if (video.getTags() != null && !video.getTags().isEmpty()) {
                builder.addAllTags(video.getTags());
            }

            // Add location
            if (video.getLatitude() != null && video.getLongitude() != null) {
                isa.jutjub.proto.Location location = isa.jutjub.proto.Location.newBuilder()
                        .setLatitude(video.getLatitude())
                        .setLongitude(video.getLongitude())
                        .setAddress(video.getLocation() != null ? video.getLocation() : "")
                        .build();
                builder.setLocation(location);
            }

            isa.jutjub.proto.VideoUploadEvent protoMessage = builder.build();
            byte[] messageBytes = protoMessage.toByteArray();

            // Send to RabbitMQ
            rabbitTemplate.send(
                    "",
                    RabbitMQConfig.PROTOBUF_QUEUE,
                    new org.springframework.amqp.core.Message(
                            messageBytes,
                            new org.springframework.amqp.core.MessageProperties()
                    )
            );

            log.debug("Published Protobuf message: {} bytes", messageBytes.length);
            return messageBytes.length;

        } catch (Exception e) {
            log.error("Error publishing Protobuf message", e);
            return 0;
        }
    }

    private VideoUploadEventDTO createEventDTO(Videos video) {
        VideoUploadEventDTO event = new VideoUploadEventDTO();
        event.setVideoId(video.getId());
        event.setTitle(video.getTitle());
        event.setDescription(video.getVideoDescription());
        event.setFileSizeBytes(video.getVideoFileSize());
        event.setDurationSeconds(video.getVideoDuration());
        event.setFormat("mp4");
        event.setUploadTimestamp(System.currentTimeMillis());

        // Tags
        if (video.getTags() != null && !video.getTags().isEmpty()) {
            event.setTags(video.getTags());
        }

        // Location
        if (video.getLatitude() != null && video.getLongitude() != null) {
            VideoUploadEventDTO.Location location = new VideoUploadEventDTO.Location();
            location.setLatitude(video.getLatitude());
            location.setLongitude(video.getLongitude());
            location.setAddress(video.getLocation());
            event.setLocation(location);
        }

        // URLs and paths
        event.setVideoUrl(buildVideoUrl(video.getId()));
        event.setThumbnailUrl(buildThumbnailUrl(video.getId()));
        event.setVideoPath(video.getVideoPath());
        event.setThumbnailPath(video.getThumbnailPath());

        // Counters
        event.setViewsCount(video.getViewsCount());
        event.setLikesCount(video.getLikesCount());
        event.setCommentsCount(video.getCommentsCount());

        return event;
    }

    private String buildVideoUrl(Long videoId) {
        return String.format("http://%s:%d/api/video-posts/%d/video",
                serverAddress, serverPort, videoId);
    }

    private String buildThumbnailUrl(Long videoId) {
        return String.format("http://%s:%d/api/video-posts/%d/thumbnail",
                serverAddress, serverPort, videoId);
    }
}