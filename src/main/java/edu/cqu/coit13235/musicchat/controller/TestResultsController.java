package edu.cqu.coit13235.musicchat.controller;

import edu.cqu.coit13235.musicchat.service.TestResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for exposing test results.
 * Provides an endpoint to retrieve the latest test execution results.
 */
@RestController
@RequestMapping("/tests")
public class TestResultsController {

    private final TestResultsService testResultsService;

    @Autowired
    public TestResultsController(TestResultsService testResultsService) {
        this.testResultsService = testResultsService;
    }

    /**
     * Get the latest test execution results.
     * GET /tests
     * 
     * @return ResponseEntity containing structured test results JSON
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTestResults() {
        Map<String, Object> results = testResultsService.getLatestTestResults();
        return ResponseEntity.ok(results);
    }
}

