package isa.jutjub.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import isa.jutjub.consumer.model.VideoUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class VideoEventConsumerApp {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoEventConsumerApp.class);
    
    // RabbitMQ Configuration
    private static final String RABBITMQ_HOST = "localhost";
    private static final int RABBITMQ_PORT = 5672;
    private static final String RABBITMQ_USER = "admin";
    private static final String RABBITMQ_PASS = "admin123";
    
    // Queue names
    private static final String JSON_QUEUE = "video.upload.json";
    private static final String PROTOBUF_QUEUE = "video.upload.protobuf";
    
    // Storage for received messages
    private static final List<MessageRecord> receivedMessages = new ArrayList<>();
    
    // Performance metrics
    private static final PerformanceStats jsonStats = new PerformanceStats("JSON");
    private static final PerformanceStats protobufStats = new PerformanceStats("Protobuf");
    
    private static Connection connection;
    private static Channel channel;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        logger.info("===========================================");
        logger.info("   Video Event Consumer - Starting Up");
        logger.info("===========================================");
        
        try {
            // Connect to RabbitMQ
            connectToRabbitMQ();
            
            // Start consuming from both queues
            startConsumers();
            
            // Start interactive console
            startConsole();
            
        } catch (Exception e) {
            logger.error("Fatal error in consumer application", e);
        } finally {
            cleanup();
        }
    }
    
    private static void connectToRabbitMQ() throws IOException, TimeoutException {
        logger.info("Connecting to RabbitMQ at {}:{}", RABBITMQ_HOST, RABBITMQ_PORT);
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setPort(RABBITMQ_PORT);
        factory.setUsername(RABBITMQ_USER);
        factory.setPassword(RABBITMQ_PASS);
        
        connection = factory.newConnection();
        channel = connection.createChannel();
        
        // Declare queues (idempotent - won't recreate if exists)
        channel.queueDeclare(JSON_QUEUE, true, false, false, null);
        channel.queueDeclare(PROTOBUF_QUEUE, true, false, false, null);
        
        logger.info("✓ Connected to RabbitMQ successfully");
        logger.info("✓ Queues declared: {} and {}", JSON_QUEUE, PROTOBUF_QUEUE);
    }
    
    private static void startConsumers() throws IOException {
        logger.info("Starting message consumers...");
        
        // JSON Consumer
        DeliverCallback jsonCallback = (consumerTag, delivery) -> {
            handleJsonMessage(delivery.getBody());
        };
        
        channel.basicConsume(JSON_QUEUE, true, jsonCallback, consumerTag -> {});
        logger.info("✓ JSON consumer started on queue: {}", JSON_QUEUE);
        
        // Protobuf Consumer
        DeliverCallback protobufCallback = (consumerTag, delivery) -> {
            handleProtobufMessage(delivery.getBody());
        };
        
        channel.basicConsume(PROTOBUF_QUEUE, true, protobufCallback, consumerTag -> {});
        logger.info("✓ Protobuf consumer started on queue: {}", PROTOBUF_QUEUE);
        
        logger.info("");
        logger.info("Listening for messages...");
        logger.info("");
    }
    
    private static void handleJsonMessage(byte[] messageBody) {
        long startTime = System.nanoTime();
        
        try {
            String jsonString = new String(messageBody, StandardCharsets.UTF_8);
            VideoUploadEvent event = objectMapper.readValue(jsonString, VideoUploadEvent.class);
            
            long endTime = System.nanoTime();
            long deserializationTimeNs = endTime - startTime;
            
            // Record statistics
            jsonStats.recordDeserialization(deserializationTimeNs, messageBody.length);
            
            // Store message
            MessageRecord record = new MessageRecord(
                "JSON",
                event.getVideoId(),
                event.getTitle(),
                messageBody.length,
                event.getVideoUrl(),
                deserializationTimeNs
            );
            receivedMessages.add(record);
            
            // Log notification
            logger.info("📨 [JSON] Received: Video #{} - '{}' | Size: {} bytes | Deserialization: {} μs | URL: {}",
                    event.getVideoId(),
                    event.getTitle(),
                    messageBody.length,
                    deserializationTimeNs / 1000,
                    event.getVideoUrl());
            
        } catch (Exception e) {
            logger.error("Error processing JSON message", e);
        }
    }
    
    private static void handleProtobufMessage(byte[] messageBody) {
        long startTime = System.nanoTime();
        
        try {
            isa.jutjub.proto.VideoUploadEvent event =
                    isa.jutjub.proto.VideoUploadEvent.parseFrom(messageBody);


            long endTime = System.nanoTime();
            long deserializationTimeNs = endTime - startTime;
            
            // Record statistics
            protobufStats.recordDeserialization(deserializationTimeNs, messageBody.length);
            
            // Store message
            MessageRecord record = new MessageRecord(
                "Protobuf",
                event.getVideoId(),
                event.getTitle(),
                messageBody.length,
                event.getVideoUrl(),
                deserializationTimeNs
            );
            receivedMessages.add(record);
            
            // Log notification
            logger.info("📨 [Protobuf] Received: Video #{} - '{}' | Size: {} bytes | Deserialization: {} μs | URL: {}",
                    event.getVideoId(),
                    event.getTitle(),
                    messageBody.length,
                    deserializationTimeNs / 1000,
                    event.getVideoUrl());
            
        } catch (Exception e) {
            logger.error("Error processing Protobuf message", e);
        }
    }
    
    private static void startConsole() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine().trim().toLowerCase();
            
            switch (command) {
                case "h":
                case "help":
                    printHelp();
                    break;
                    
                case "l":
                case "list":
                    listMessages();
                    break;
                    
                case "s":
                case "stats":
                    printStatistics();
                    break;
                    
                case "c":
                case "compare":
                    comparePerformance();
                    break;
                    
                case "clear":
                    receivedMessages.clear();
                    jsonStats.reset();
                    protobufStats.reset();
                    System.out.println("✓ Cleared all messages and statistics");
                    break;
                    
                case "q":
                case "quit":
                case "exit":
                    running = false;
                    break;
                    
                default:
                    System.out.println("Unknown command. Type 'h' for help.");
            }
        }
        
        scanner.close();
    }
    
    private static void printHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("h, help      - Show this help");
        System.out.println("l, list      - List all received messages");
        System.out.println("s, stats     - Show performance statistics");
        System.out.println("c, compare   - Compare JSON vs Protobuf performance");
        System.out.println("clear        - Clear all messages and stats");
        System.out.println("q, quit      - Exit application");
        System.out.println();
    }
    
    private static void listMessages() {
        if (receivedMessages.isEmpty()) {
            System.out.println("No messages received yet.");
            return;
        }
        
        System.out.println("\n=== Received Messages (" + receivedMessages.size() + ") ===");
        System.out.println(String.format("%-10s %-10s %-30s %-15s %-15s %-50s",
                "Format", "Video ID", "Title", "Msg Size", "Deser Time", "Video URL"));
        System.out.println("-".repeat(140));
        
        for (MessageRecord record : receivedMessages) {
            System.out.println(String.format("%-10s %-10d %-30s %-15s %-15s %-50s",
                    record.format,
                    record.videoId,
                    truncate(record.title, 28),
                    formatBytes(record.messageSize),
                    record.deserializationTimeNs / 1000 + " μs",
                    truncate(record.videoUrl, 48)));
        }
        System.out.println();
    }
    
    private static void printStatistics() {
        System.out.println("\n=== Performance Statistics ===");
        System.out.println();
        System.out.println("JSON:");
        jsonStats.print();
        System.out.println();
        System.out.println("Protobuf:");
        protobufStats.print();
        System.out.println();
    }
    
    private static void comparePerformance() {
        if (jsonStats.count == 0 && protobufStats.count == 0) {
            System.out.println("No data to compare yet.");
            return;
        }
        
        System.out.println("\n=== JSON vs Protobuf Comparison ===");
        System.out.println();
        
        System.out.println(String.format("%-30s %-20s %-20s %-20s",
                "Metric", "JSON", "Protobuf", "Winner"));
        System.out.println("-".repeat(90));
        
        // Message count
        System.out.println(String.format("%-30s %-20d %-20d %-20s",
                "Messages Received",
                jsonStats.count,
                protobufStats.count,
                "-"));
        
        // Avg deserialization time
        if (jsonStats.count > 0 && protobufStats.count > 0) {
            double jsonAvg = jsonStats.avgDeserializationTimeNs / 1000.0;
            double protobufAvg = protobufStats.avgDeserializationTimeNs / 1000.0;
            String winner = jsonAvg < protobufAvg ? "JSON ✓" : "Protobuf ✓";
            
            System.out.println(String.format("%-30s %-20s %-20s %-20s",
                    "Avg Deserialization (μs)",
                    String.format("%.2f", jsonAvg),
                    String.format("%.2f", protobufAvg),
                    winner));
        }
        
        // Avg message size
        if (jsonStats.count > 0 && protobufStats.count > 0) {
            String winner = jsonStats.avgMessageSize < protobufStats.avgMessageSize ? "JSON ✓" : "Protobuf ✓";
            
            System.out.println(String.format("%-30s %-20s %-20s %-20s",
                    "Avg Message Size",
                    formatBytes((long) jsonStats.avgMessageSize),
                    formatBytes((long) protobufStats.avgMessageSize),
                    winner));
        }
        
        // Size reduction
        if (jsonStats.count > 0 && protobufStats.count > 0) {
            double reduction = ((jsonStats.avgMessageSize - protobufStats.avgMessageSize) 
                    / jsonStats.avgMessageSize) * 100;
            
            System.out.println(String.format("%-30s %-20s",
                    "Protobuf Size Reduction",
                    String.format("%.2f%%", reduction)));
        }
        
        System.out.println();
    }
    
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
    
    private static void cleanup() {
        logger.info("Shutting down...");
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
        logger.info("Goodbye!");
    }
    
    // Inner classes for data storage
    
    private static class MessageRecord {
        String format;
        long videoId;
        String title;
        long messageSize;
        String videoUrl;
        long deserializationTimeNs;
        
        MessageRecord(String format, long videoId, String title, 
                     long messageSize, String videoUrl, long deserializationTimeNs) {
            this.format = format;
            this.videoId = videoId;
            this.title = title;
            this.messageSize = messageSize;
            this.videoUrl = videoUrl;
            this.deserializationTimeNs = deserializationTimeNs;
        }
    }
    
    private static class PerformanceStats {
        String format;
        int count = 0;
        long totalDeserializationTimeNs = 0;
        long totalMessageSize = 0;
        double avgDeserializationTimeNs = 0;
        double avgMessageSize = 0;
        
        PerformanceStats(String format) {
            this.format = format;
        }
        
        void recordDeserialization(long timeNs, long messageSize) {
            count++;
            totalDeserializationTimeNs += timeNs;
            totalMessageSize += messageSize;
            avgDeserializationTimeNs = totalDeserializationTimeNs / (double) count;
            avgMessageSize = totalMessageSize / (double) count;
        }
        
        void reset() {
            count = 0;
            totalDeserializationTimeNs = 0;
            totalMessageSize = 0;
            avgDeserializationTimeNs = 0;
            avgMessageSize = 0;
        }
        
        void print() {
            System.out.println("  Messages: " + count);
            System.out.println("  Avg Deserialization Time: " + String.format("%.2f μs", avgDeserializationTimeNs / 1000.0));
            System.out.println("  Avg Message Size: " + formatBytes((long) avgMessageSize));
        }
    }
}
