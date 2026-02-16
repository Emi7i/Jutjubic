const http = require('http');
const { Agent } = require('http');

// Configuration
const TARGET_URL = 'http://localhost:8080/api/test/load';
const CONCURRENT_REQUESTS = 500;
const TEST_DURATION_SECONDS = 30;
const REQUEST_INTERVAL_MS = 1000 / CONCURRENT_REQUESTS; // ~2ms between requests

// Create HTTP agent with connection pooling for better performance
const agent = new Agent({
    keepAlive: true,
    maxSockets: 100,
    maxFreeSockets: 10,
    timeout: 2000,
    keepAliveMsecs: 1000
});

let totalRequests = 0;
let successfulRequests = 0;
let failedRequests = 0;
let startTime = Date.now();
let activeRequests = 0;

console.log(`Starting optimized load test: ${CONCURRENT_REQUESTS} req/s for ${TEST_DURATION_SECONDS} seconds`);
console.log(`Target: ${TARGET_URL}`);

function makeRequest() {
    return new Promise((resolve) => {
        activeRequests++;
        totalRequests++;
        
        const req = http.get(TARGET_URL, { agent: agent }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                activeRequests--;
                if (res.statusCode === 200) {
                    successfulRequests++;
                } else {
                    failedRequests++;
                }
                resolve();
            });
        });
        
        req.on('error', () => {
            activeRequests--;
            failedRequests++;
            resolve();
        });
        
        req.setTimeout(2000, () => {
            req.destroy();
            activeRequests--;
            failedRequests++;
            resolve();
        });
    });
}

async function runLoadTest() {
    const endTime = startTime + (TEST_DURATION_SECONDS * 1000);
    const requestPromises = [];
    
    while (Date.now() < endTime) {
        requestPromises.push(makeRequest());
        await new Promise(resolve => setTimeout(resolve, REQUEST_INTERVAL_MS));
    }
    
    // Wait for all remaining requests to complete
    await Promise.all(requestPromises);
    
    const actualDuration = (Date.now() - startTime) / 1000;
    const actualRPS = totalRequests / actualDuration;
    
    console.log('\n=== Load Test Results ===');
    console.log(`Duration: ${actualDuration.toFixed(2)} seconds`);
    console.log(`Total Requests: ${totalRequests}`);
    console.log(`Successful: ${successfulRequests}`);
    console.log(`Failed: ${failedRequests}`);
    console.log(`Actual RPS: ${actualRPS.toFixed(2)}`);
    console.log(`Success Rate: ${((successfulRequests / totalRequests) * 100).toFixed(2)}%`);
    console.log('\nCheck Grafana at http://localhost:3000 for metrics visualization');
}

runLoadTest().catch(console.error);
