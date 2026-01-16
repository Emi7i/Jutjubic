# Simple PowerShell script to test concurrent video views
# Simulates multiple users visiting backend API to increment view count
#
# Usage: .\test-concurrent-views.ps1 -Threads 10 -ViewsPerThread 5
# Example: .\test-concurrent-views.ps1 -Threads 10 -ViewsPerThread 5

param(
    [int]$Threads = 10,
    [int]$ViewsPerThread = 5,
    [string]$VideoUrl = "http://localhost:8080/api/video-posts/6",
    [int]$DelayBetweenRequests = 100
)

# Statistics
$script:SuccessfulRequests = 0
$script:FailedRequests = 0
$script:StartTime = Get-Date

Write-Host "Concurrent Video Views Test" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "Video URL: $VideoUrl" -ForegroundColor White
Write-Host "Threads: $Threads" -ForegroundColor White
Write-Host "Views per thread: $ViewsPerThread" -ForegroundColor White
Write-Host "Total expected views: $($Threads * $ViewsPerThread)" -ForegroundColor White
Write-Host ""

function Visit-Video {
    try {
        $response = Invoke-WebRequest -Uri $VideoUrl -Method GET -TimeoutSec 5 -UseBasicParsing
        
        if ($response.StatusCode -eq 200) {
            $script:SuccessfulRequests++
            Write-Host "View #$($script:SuccessfulRequests) - Status: $($response.StatusCode)" -ForegroundColor Green
        } else {
            $script:FailedRequests++
            Write-Host "View failed - Status: $($response.StatusCode)" -ForegroundColor Red
        }
    }
    catch {
        $script:FailedRequests++
        Write-Host "Request error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Simulate-User($ThreadId) {
    Write-Host "User $ThreadId started" -ForegroundColor Yellow
    
    for ($i = 0; $i -lt $ViewsPerThread; $i++) {
        Visit-Video
        
        # Small delay between requests
        if ($i -lt $ViewsPerThread - 1) {
            Start-Sleep -Milliseconds $DelayBetweenRequests
        }
    }
    
    Write-Host "User $ThreadId finished" -ForegroundColor Yellow
}

# Main test function
function Run-Test {
    Write-Host "Starting concurrent test..." -ForegroundColor Cyan
    Write-Host ""
    
    # Create jobs for each thread
    $jobs = @()
    
    for ($i = 1; $i -le $Threads; $i++) {
        $job = Start-Job -ScriptBlock {
            param($ThreadId, $ViewsPerThread, $VideoUrl, $DelayBetweenRequests)
            
            # Import functions into job
            function Visit-Video {
                try {
                    $response = Invoke-WebRequest -Uri $VideoUrl -Method GET -TimeoutSec 5 -UseBasicParsing
                    
                    if ($response.StatusCode -eq 200) {
                        $global:SuccessfulRequests++
                        Write-Host "View #$($global:SuccessfulRequests) - Status: $($response.StatusCode)" -ForegroundColor Green
                    } else {
                        $global:FailedRequests++
                        Write-Host "View failed - Status: $($response.StatusCode)" -ForegroundColor Red
                    }
                }
                catch {
                    $global:FailedRequests++
                    Write-Host "Request error: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
            
            # Initialize counters in job scope
            $global:SuccessfulRequests = 0
            $global:FailedRequests = 0
            
            Write-Host "User $ThreadId started" -ForegroundColor Yellow
            
            for ($i = 0; $i -lt $ViewsPerThread; $i++) {
                Visit-Video
                
                if ($i -lt $ViewsPerThread - 1) {
                    Start-Sleep -Milliseconds $DelayBetweenRequests
                }
            }
            
            Write-Host "User $ThreadId finished" -ForegroundColor Yellow
            
            # Return results
            return @{
                Successful = $global:SuccessfulRequests
                Failed = $global:FailedRequests
            }
        } -ArgumentList $i, $ViewsPerThread, $VideoUrl, $DelayBetweenRequests
        
        $jobs += $job
    }
    
    # Wait for all jobs to complete
    $results = $jobs | Wait-Job | Receive-Job
    
    # Aggregate results
    $totalSuccessful = ($results | ForEach-Object { $_.Successful } | Measure-Object -Sum).Sum
    $totalFailed = ($results | ForEach-Object { $_.Failed } | Measure-Object -Sum).Sum
    
    # Clean up jobs
    $jobs | Remove-Job
    
    # Calculate duration
    $endTime = Get-Date
    $duration = ($endTime - $script:StartTime).TotalMilliseconds
    $totalRequests = $totalSuccessful + $totalFailed
    $expectedRequests = $Threads * $ViewsPerThread
    
    # Show results
    Write-Host ""
    Write-Host "TEST RESULTS" -ForegroundColor Cyan
    Write-Host "================" -ForegroundColor Cyan
    Write-Host "Duration: $([int]$duration)ms" -ForegroundColor White
    Write-Host "Successful requests: $totalSuccessful" -ForegroundColor Green
    Write-Host "Failed requests: $totalFailed" -ForegroundColor Red
    Write-Host "Total requests: $totalRequests" -ForegroundColor White
    Write-Host "Expected requests: $expectedRequests" -ForegroundColor White
    Write-Host "Requests per second: $([math]::Round($totalRequests / ($duration / 1000), 2))" -ForegroundColor White
    
    $testPassed = ($totalSuccessful -eq $expectedRequests) -and ($totalFailed -eq 0)
    Write-Host ""
    if ($testPassed) {
        Write-Host "TEST PASSED" -ForegroundColor Green
        Write-Host "All concurrent views were successful!" -ForegroundColor Green
        Write-Host "The view counter should be incremented by $expectedRequests" -ForegroundColor Green
    } else {
        Write-Host "TEST FAILED" -ForegroundColor Red
        Write-Host "Some requests failed. Check if the frontend is running." -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Check the video page to see the updated view count!" -ForegroundColor Cyan
    Write-Host "Visit: $VideoUrl" -ForegroundColor Cyan
}

# Error handling
trap {
    Write-Host "Script error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Run the test
try {
    Run-Test
}
catch {
    Write-Host "Test failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
