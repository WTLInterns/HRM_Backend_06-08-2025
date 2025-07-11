package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.ZKTecoTcpListenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime-listener")
@CrossOrigin(origins = "*")
public class RealTimeListenerController {

    @Autowired
    private ZKTecoTcpListenerService tcpListenerService;

    /**
     * Get TCP listener status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getListenerStatus() {
        try {
            Map<String, Object> status = tcpListenerService.getListenerStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting listener status: " + e.getMessage());
        }
    }

    /**
     * Test endpoint to simulate punch data and process it
     */
    @PostMapping("/test-punch")
    public ResponseEntity<?> testPunch(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String timeStr = (String) request.get("time");
            String status = (String) request.get("status");

            // Simulate receiving data from F22 and process it
            System.out.println("Test punch received: User " + userId + " at " + timeStr + " status " + status);

            // Process the punch data (simulate TCP data processing)
            String deviceIp = "192.168.1.201"; // Simulate F22 IP
            String simulatedData = userId + "\t" + timeStr + "\t" + status + "\t1";

            // Call the TCP listener's data processing method
            boolean processed = tcpListenerService.processTestPunchData(simulatedData, deviceIp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message",
                    processed ? "Test punch processed and saved to database" : "Test punch received but not processed");
            response.put("userId", userId);
            response.put("time", timeStr);
            response.put("status", status);
            response.put("processed", processed);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing test punch: " + e.getMessage());
        }
    }
}
