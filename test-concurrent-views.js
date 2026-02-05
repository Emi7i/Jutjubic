#!/usr/bin/env node

/**
 * Simple script to test concurrent video views
 * Simulates multiple users visiting http://localhost:4200/videos/1
 * 
 * Usage: node test-concurrent-views.js [threads] [viewsPerThread]
 * Example: node test-concurrent-views.js 10 5
 */

const http = require('http');
const https = require('https');

// Configuration
const VIDEO_URL = 'http://localhost:8080/api/video-posts/6';
const THREADS = parseInt(process.argv[2]) || 10;
const VIEWS_PER_THREAD = parseInt(process.argv[3]) || 5;
const DELAY_BETWEEN_REQUESTS = 100; // ms

// Statistics
let successfulRequests = 0;
let failedRequests = 0;
let startTime = Date.now();

console.log('Concurrent Video Views Test');
console.log('================================');
console.log(`Video URL: ${VIDEO_URL}`);
console.log(`Threads: ${THREADS}`);
console.log(`Views per thread: ${VIEWS_PER_THREAD}`);
console.log(`Total expected views: ${THREADS * VIEWS_PER_THREAD}`);
console.log('');

/**
 * Make HTTP request to increment view count
 */
function visitVideo() {
    return new Promise((resolve) => {
        const protocol = VIDEO_URL.startsWith('https') ? https : http;
        
        const req = protocol.get(VIDEO_URL, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                if (res.statusCode === 200) {
                    successfulRequests++;
                    console.log(`View #${successfulRequests} - Status: ${res.statusCode}`);
                } else {
                    failedRequests++;
                    console.log(`View failed - Status: ${res.statusCode}`);
                }
                resolve();
            });
        });
        
        req.on('error', (err) => {
            failedRequests++;
            console.log(`Request error: ${err.message}`);
            resolve();
        });
        
        req.setTimeout(5000, () => {
            req.destroy();
            failedRequests++;
            console.log(`Request timeout`);
            resolve();
        });
    });
}

/**
 * Simulate one user visiting video multiple times
 */
async function simulateUser(threadId) {
    console.log(`User ${threadId} started`);
    
    for (let i = 0; i < VIEWS_PER_THREAD; i++) {
        await visitVideo();
        
        // Small delay between requests
        if (i < VIEWS_PER_THREAD - 1) {
            await new Promise(resolve => setTimeout(resolve, DELAY_BETWEEN_REQUESTS));
        }
    }
    
    console.log(`User ${threadId} finished`);
}

/**
 * Main test function
 */
async function runTest() {
    console.log('Starting concurrent test...\n');
    
    const promises = [];
    
    // Start all threads
    for (let i = 1; i <= THREADS; i++) {
        promises.push(simulateUser(i));
    }
    
    // Wait for all threads to complete
    await Promise.all(promises);
    
    // Show results
    const endTime = Date.now();
    const duration = endTime - startTime;
    const totalRequests = successfulRequests + failedRequests;
    const expectedRequests = THREADS * VIEWS_PER_THREAD;
    
    console.log('\nTEST RESULTS');
    console.log('================');
    console.log(`Duration: ${duration}ms`);
    console.log(`Successful requests: ${successfulRequests}`);
    console.log(`Failed requests: ${failedRequests}`);
    console.log(`Total requests: ${totalRequests}`);
    console.log(`Expected requests: ${expectedRequests}`);
    console.log(`Requests per second: ${(totalRequests / (duration / 1000)).toFixed(2)}`);
    
    const testPassed = successfulRequests === expectedRequests && failedRequests === 0;
    console.log(`\n${testPassed ? 'TEST PASSED' : 'TEST FAILED'}`);
    
    if (testPassed) {
        console.log('All concurrent views were successful!');
        console.log('The view counter should be incremented by ' + expectedRequests);
    } else {
        console.log('Some requests failed. Check if the frontend is running.');
    }
    
    console.log('\nCheck the video page to see the updated view count!');
    console.log(`Visit: ${VIDEO_URL}`);
}

// Handle uncaught errors
process.on('uncaughtException', (err) => {
    console.error('Uncaught error:', err.message);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled rejection:', reason);
    process.exit(1);
});

// Run the test
runTest().catch(err => {
    console.error('Test failed:', err.message);
    process.exit(1);
});
