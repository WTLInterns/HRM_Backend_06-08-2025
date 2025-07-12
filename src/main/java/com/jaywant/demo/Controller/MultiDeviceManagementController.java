package com.jaywant.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/multi-device")
@CrossOrigin(origins = "*")
public class MultiDeviceManagementController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get all registered devices and their status
     */
    @GetMapping("/devices")
    public ResponseEntity<?> getAllDevices() {
        try {
            String sql = """
                SELECT DISTINCT 
                    terminal_sn,
                    terminal_alias,
                    COUNT(*) as total_transactions,
                    MAX(punch_time) as last_activity,
                    MIN(punch_time) as first_activity
                FROM easywdms.iclock_transaction 
                GROUP BY terminal_sn, terminal_alias
                ORDER BY last_activity DESC
                """;

            List<Map<String, Object>> devices = jdbcTemplate.queryForList(sql);
            
            // Add status information
            for (Map<String, Object> device : devices) {
                Object lastActivityObj = device.get("last_activity");
                if (lastActivityObj != null) {
                    LocalDateTime lastActivity = LocalDateTime.parse(lastActivityObj.toString().replace("T", " "), 
                                                                   DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    LocalDateTime now = LocalDateTime.now();
                    long minutesSinceLastActivity = java.time.Duration.between(lastActivity, now).toMinutes();
                    
                    if (minutesSinceLastActivity < 5) {
                        device.put("status", "ONLINE");
                        device.put("status_color", "green");
                    } else if (minutesSinceLastActivity < 60) {
                        device.put("status", "IDLE");
                        device.put("status_color", "yellow");
                    } else {
                        device.put("status", "OFFLINE");
                        device.put("status_color", "red");
                    }
                    device.put("minutes_since_last_activity", minutesSinceLastActivity);
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "devices", devices,
                "total_devices", devices.size(),
                "checked_at", LocalDateTime.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get device activity for specific device
     */
    @GetMapping("/devices/{deviceSerial}/activity")
    public ResponseEntity<?> getDeviceActivity(@PathVariable String deviceSerial) {
        try {
            String sql = """
                SELECT 
                    id, emp_code, punch_time, punch_state, terminal_sn, terminal_alias
                FROM easywdms.iclock_transaction 
                WHERE terminal_sn = ?
                ORDER BY punch_time DESC
                LIMIT 50
                """;

            List<Map<String, Object>> activity = jdbcTemplate.queryForList(sql, deviceSerial);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "device_serial", deviceSerial,
                "recent_activity", activity,
                "activity_count", activity.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get today's statistics for all devices
     */
    @GetMapping("/stats/today")
    public ResponseEntity<?> getTodayStats() {
        try {
            String sql = """
                SELECT 
                    terminal_sn,
                    terminal_alias,
                    COUNT(*) as punches_today,
                    COUNT(DISTINCT emp_code) as unique_employees,
                    MIN(punch_time) as first_punch,
                    MAX(punch_time) as last_punch
                FROM easywdms.iclock_transaction 
                WHERE DATE(punch_time) = CURDATE()
                GROUP BY terminal_sn, terminal_alias
                ORDER BY punches_today DESC
                """;

            List<Map<String, Object>> stats = jdbcTemplate.queryForList(sql);

            // Get total stats
            String totalSql = """
                SELECT 
                    COUNT(*) as total_punches_today,
                    COUNT(DISTINCT emp_code) as total_unique_employees,
                    COUNT(DISTINCT terminal_sn) as active_devices_today
                FROM easywdms.iclock_transaction 
                WHERE DATE(punch_time) = CURDATE()
                """;

            Map<String, Object> totals = jdbcTemplate.queryForMap(totalSql);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "device_stats", stats,
                "totals", totals,
                "date", LocalDateTime.now().toLocalDate()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Register a new device or update existing device info
     */
    @PostMapping("/devices/register")
    public ResponseEntity<?> registerDevice(@RequestBody Map<String, Object> deviceInfo) {
        try {
            String deviceSerial = (String) deviceInfo.get("device_serial");
            String deviceAlias = (String) deviceInfo.get("device_alias");
            String location = (String) deviceInfo.get("location");
            String branchName = (String) deviceInfo.get("branch_name");

            if (deviceSerial == null || deviceSerial.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "device_serial is required"
                ));
            }

            // For now, we'll just return success since device registration
            // happens automatically when devices send data
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Device registration noted",
                "device_serial", deviceSerial,
                "device_alias", deviceAlias,
                "location", location,
                "branch_name", branchName,
                "registered_at", LocalDateTime.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check for multi-device system
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            // Check database connectivity
            String sql = "SELECT COUNT(*) as transaction_count FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()";
            Integer todayCount = jdbcTemplate.queryForObject(sql, Integer.class);

            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "MultiDeviceManagementController",
                "database_connection", "OK",
                "transactions_today", todayCount,
                "checked_at", LocalDateTime.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get device sync status and recommendations
     */
    @GetMapping("/sync-status")
    public ResponseEntity<?> getSyncStatus() {
        try {
            // Check recent sync activity
            String sql = """
                SELECT 
                    terminal_sn,
                    COUNT(*) as recent_transactions,
                    MAX(punch_time) as last_transaction
                FROM easywdms.iclock_transaction 
                WHERE punch_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
                GROUP BY terminal_sn
                """;

            List<Map<String, Object>> recentActivity = jdbcTemplate.queryForList(sql);

            List<String> recommendations = new ArrayList<>();
            
            if (recentActivity.isEmpty()) {
                recommendations.add("No recent device activity detected. Check device connectivity.");
            } else {
                recommendations.add("System is processing data from " + recentActivity.size() + " active device(s)");
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "recent_activity", recentActivity,
                "recommendations", recommendations,
                "sync_service_status", "RUNNING",
                "last_checked", LocalDateTime.now()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
