package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.SimpleRealtimeBiometricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simple-realtime")
@CrossOrigin(origins = "*")
public class SimpleRealtimeBiometricController {

    @Autowired
    private SimpleRealtimeBiometricService realtimeService;

    /**
     * Get sync statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSyncStats() {
        try {
            Map<String, Object> stats = realtimeService.getSyncStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Trigger manual sync
     */
    @PostMapping("/sync-now")
    public ResponseEntity<?> triggerManualSync() {
        try {
            Map<String, Object> result = realtimeService.triggerManualSync();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "SimpleRealtimeBiometricService",
                "message", "Real-time sync service is running"));
    }

    /**
     * Trigger auto punch-out process manually
     */
    @PostMapping("/auto-punch-out")
    public ResponseEntity<?> triggerAutoPunchOut() {
        try {
            realtimeService.autoProcessMissingPunchOut();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Auto punch-out process completed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
