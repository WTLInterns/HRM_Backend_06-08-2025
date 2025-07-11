package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.SimpleBiometricTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simple-biometric-test")
@CrossOrigin(origins = "*")
public class SimpleBiometricTestController {

    @Autowired
    private SimpleBiometricTestService testService;

    /**
     * Test iclock data processing with simple logic
     */
    @PostMapping("/test-processing")
    public ResponseEntity<?> testProcessing() {
        try {
            Map<String, Object> result = testService.testIclockProcessing();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get simple statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = testService.getSimpleStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "service", "SimpleBiometricTestService",
            "message", "Service is running"
        ));
    }
}
