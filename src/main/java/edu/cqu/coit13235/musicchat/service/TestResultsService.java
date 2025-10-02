package edu.cqu.coit13235.musicchat.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking and reporting test results.
 * Maintains the latest test execution results in memory.
 */
@Service
public class TestResultsService {

    private final Map<String, Object> latestTestResults = new ConcurrentHashMap<>();

    public TestResultsService() {
        // Initialize with default values
        // Note: ConcurrentHashMap does not allow null values
        latestTestResults.put("lastRun", "NOT_RUN");
        latestTestResults.put("totalTests", 0);
        latestTestResults.put("passed", 0);
        latestTestResults.put("failed", 0);
        latestTestResults.put("skipped", 0);
        latestTestResults.put("status", "NOT_RUN");
    }

    /**
     * Update test results with latest execution data.
     * 
     * @param totalTests Total number of tests executed
     * @param passed Number of tests that passed
     * @param failed Number of tests that failed
     * @param skipped Number of tests that were skipped
     */
    public void updateTestResults(int totalTests, int passed, int failed, int skipped) {
        latestTestResults.put("lastRun", LocalDateTime.now().toString());
        latestTestResults.put("totalTests", totalTests);
        latestTestResults.put("passed", passed);
        latestTestResults.put("failed", failed);
        latestTestResults.put("skipped", skipped);
        latestTestResults.put("status", failed == 0 ? "PASSED" : "FAILED");
    }

    /**
     * Get the latest test results.
     * 
     * @return Map containing test execution results
     */
    public Map<String, Object> getLatestTestResults() {
        return new HashMap<>(latestTestResults);
    }

    /**
     * Reset test results to initial state.
     */
    public void resetTestResults() {
        latestTestResults.put("lastRun", "NOT_RUN");
        latestTestResults.put("totalTests", 0);
        latestTestResults.put("passed", 0);
        latestTestResults.put("failed", 0);
        latestTestResults.put("skipped", 0);
        latestTestResults.put("status", "NOT_RUN");
    }
}

