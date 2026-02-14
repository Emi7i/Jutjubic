@echo off
echo Building and testing rate limiting with Docker...

REM Build the application
call mvn clean package -DskipTests

REM Start services
docker-compose up -d

echo Waiting for services to start...
timeout /t 30

echo.
echo Testing rate limiting endpoints...

echo 1. Testing search endpoint (100 requests/min limit)
for /l %%i in (1,1,110) do (
    curl -s -o nul -w "%%{http_code}" "http://localhost:8080/api/video-posts" > response.txt
    set /p response=<response.txt
    if "!response!"=="200" (
        echo Request %%i: OK
    ) else if "!response!"=="429" (
        echo Request %%i: RATE LIMITED
        goto :end_search
    ) else (
        echo Request %%i: ERROR (!response!)
    )
    timeout /t 1 >nul
)

:end_search

echo.
echo 2. Testing upload endpoint (10 requests/min limit)
for /l %%i in (1,1,15) do (
    curl -s -o nul -w "%%{http_code}" "http://localhost:8080/api/video-posts/upload" -H "Content-Type: application/json" -d "{\"title\":\"test\",\"videoDescription\":\"test\"}" > response.txt
    set /p response=<response.txt
    if "!response!"=="200" (
        echo Upload %%i: OK
    ) else if "!response!"=="429" (
        echo Upload %%i: RATE LIMITED
        goto :end_upload
    ) else (
        echo Upload %%i: ERROR (!response!)
    )
    timeout /t 2 >nul
)

:end_upload

echo.
echo 3. Checking Redis keys
docker exec jutjubic_backend_redis_1 redis-cli keys "*"

echo.
echo 4. Stopping services
docker-compose down

del response.txt
echo Test completed!
pause
