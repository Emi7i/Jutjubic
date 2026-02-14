# Sliding Window Rate Limiting with Docker

## Quick Start

1. **Build and run with Docker:**
   ```bash
   docker-compose up -d
   ```

2. **Test rate limiting:**
   ```bash
   # Windows
   test-docker.bat
   
   # Linux/Mac
   chmod +x test-docker.sh
   ./test-docker.sh
   ```

## Rate Limits (Sliding Window)

- **Video Upload**: 10 requests/minute
- **Video Search**: 100 requests/minute  
- **Video View**: 50 requests/minute
- **Video Like**: 20 requests/minute
- **General API**: 100 requests/minute

## Manual Testing

### Test Endpoints:
- `GET http://localhost:8080/api/video-posts` - Search rate limit
- `POST http://localhost:8080/api/video-posts/upload` - Upload rate limit
- `GET http://localhost:8080/api/video-posts/{id}` - View rate limit
- `POST http://localhost:8080/api/video-posts/{id}/like` - Like rate limit

### Expected Responses:
- **200 OK** - Request allowed
- **429 Too Many Requests** - Rate limit exceeded
- **500 Error** - Service/Redis unavailable

## Docker Services

- **Redis**: localhost:6379 (distributed rate limiting)
- **PostgreSQL**: localhost:5432 (database)
- **App**: localhost:8080 (Spring Boot application)

## Monitor Rate Limiting

Check Redis keys:
```bash
docker exec jutjubic_backend_redis_1 redis-cli keys "*"
```

View rate limit data:
```bash
docker exec jutjubic_backend_redis_1 redis-cli zrange "video_search:127.0.0.1" 0 -1 WITHSCORES
```

## Stop Services
```bash
docker-compose down
```
