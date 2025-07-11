package com.jaywant.demo.Controller;

import com.jaywant.demo.Config.EasyTimeProConfig;
import com.jaywant.demo.Config.BiometricConfig;
import com.jaywant.demo.Service.EasyTimeProSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/easytimepro-integration")
@CrossOrigin(origins = "*")
public class EasyTimeProIntegrationController {

    @Autowired
    private EasyTimeProConfig easyTimeProConfig;

    @Autowired
    private BiometricConfig biometricConfig;

    @Autowired
    private EasyTimeProSyncService syncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get integration status and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<?> getIntegrationStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Configuration status
            status.put("easyTimeProEnabled", easyTimeProConfig.getSync().isEnabled());
            status.put("middlewareEnabled", easyTimeProConfig.getMiddleware().isEnabled());
            status.put("autoSyncEnabled", easyTimeProConfig.getMiddleware().isAutoSync());
            status.put("syncInterval", easyTimeProConfig.getMiddleware().getSyncInterval());
            status.put("biometricRealtimeEnabled", biometricConfig.getRealtime().isEnabled());
            status.put("tcpPort", biometricConfig.getRealtime().getTcpPort());
            
            // Database connectivity
            Map<String, Object> dbStatus = new HashMap<>();
            try {
                Integer newHrmCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM new_hrm.iclock_transaction WHERE DATE(punch_time) = CURDATE()", Integer.class);
                dbStatus.put("newHrmConnection", "OK");
                dbStatus.put("newHrmTodayRecords", newHrmCount);
            } catch (Exception e) {
                dbStatus.put("newHrmConnection", "FAILED: " + e.getMessage());
            }

            try {
                Integer easywdmsCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()", Integer.class);
                dbStatus.put("easywdmsConnection", "OK");
                dbStatus.put("easywdmsTodayRecords", easywdmsCount);
            } catch (Exception e) {
                dbStatus.put("easywdmsConnection", "FAILED: " + e.getMessage());
            }

            try {
                Integer attendanceCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'", Integer.class);
                dbStatus.put("attendanceConnection", "OK");
                dbStatus.put("processedTodayRecords", attendanceCount);
            } catch (Exception e) {
                dbStatus.put("attendanceConnection", "FAILED: " + e.getMessage());
            }

            status.put("databaseStatus", dbStatus);
            status.put("lastChecked", LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting integration status: " + e.getMessage());
        }
    }

    /**
     * Trigger manual sync from EasyTimePro middleware
     */
    @PostMapping("/sync-now")
    public ResponseEntity<?> triggerManualSync() {
        try {
            Map<String, Object> result = syncService.manualSync();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error triggering manual sync: " + e.getMessage());
        }
    }

    /**
     * Get real-time data flow from EasyTimePro
     */
    @GetMapping("/realtime-data")
    public ResponseEntity<?> getRealtimeData(@RequestParam(defaultValue = "10") int limit) {
        try {
            // Get latest transactions from EasyTimePro middleware
            String sql = """
                SELECT 
                    emp_code, punch_time, punch_state, verify_type, 
                    terminal_sn, upload_time, source
                FROM easywdms.iclock_transaction 
                WHERE punch_time >= NOW() - INTERVAL 1 HOUR
                ORDER BY punch_time DESC 
                LIMIT ?
                """;

            List<Map<String, Object>> realtimeData = jdbcTemplate.queryForList(sql, limit);

            // Get corresponding processed attendance
            String attendanceSql = """
                SELECT 
                    a.employee_id, e.fullName, a.date, a.punch_in_time, 
                    a.punch_out_time, a.biometric_user_id, a.device_serial,
                    a.attendance_source, a.created_at
                FROM new_hrm.attendance a
                JOIN new_hrm.employee e ON a.employee_id = e.empId
                WHERE a.date = CURDATE() AND a.attendance_source = 'BIOMETRIC'
                ORDER BY a.created_at DESC
                LIMIT ?
                """;

            List<Map<String, Object>> processedAttendance = jdbcTemplate.queryForList(attendanceSql, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("realtimeTransactions", realtimeData);
            response.put("processedAttendance", processedAttendance);
            response.put("dataFlowActive", !realtimeData.isEmpty());
            response.put("lastUpdate", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting realtime data: " + e.getMessage());
        }
    }

    /**
     * Test biometric data processing pipeline
     */
    @PostMapping("/test-pipeline")
    public ResponseEntity<?> testBiometricPipeline(@RequestBody Map<String, Object> request) {
        try {
            String empCode = (String) request.get("empCode");
            String punchTime = (String) request.get("punchTime");
            Integer punchState = (Integer) request.get("punchState");
            String terminalSn = (String) request.get("terminalSn");

            if (empCode == null || punchTime == null || terminalSn == null) {
                return ResponseEntity.badRequest().body("empCode, punchTime, and terminalSn are required");
            }

            // Insert test data into EasyTimePro middleware
            String insertSql = """
                INSERT INTO easywdms.iclock_transaction 
                (emp_code, punch_time, punch_state, verify_type, terminal_sn, source, upload_time)
                VALUES (?, ?, ?, 1, ?, 'TEST', NOW())
                """;

            jdbcTemplate.update(insertSql, empCode, punchTime, punchState != null ? punchState : 0, terminalSn);

            // Trigger sync
            Map<String, Object> syncResult = syncService.manualSync();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test data inserted and sync triggered");
            response.put("testData", request);
            response.put("syncResult", syncResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error testing pipeline: " + e.getMessage());
        }
    }

    /**
     * Get biometric data flow statistics
     */
    @GetMapping("/flow-stats")
    public ResponseEntity<?> getDataFlowStats(@RequestParam(required = false) String date) {
        try {
            String targetDate = date != null ? date : LocalDateTime.now().toLocalDate().toString();

            Map<String, Object> stats = new HashMap<>();

            // EasyTimePro middleware stats
            String middlewareSql = """
                SELECT 
                    COUNT(*) as total_transactions,
                    COUNT(DISTINCT emp_code) as unique_employees,
                    COUNT(DISTINCT terminal_sn) as unique_terminals,
                    MIN(punch_time) as first_punch,
                    MAX(punch_time) as last_punch
                FROM easywdms.iclock_transaction 
                WHERE DATE(punch_time) = ?
                """;

            Map<String, Object> middlewareStats = jdbcTemplate.queryForMap(middlewareSql, targetDate);

            // Processed attendance stats
            String attendanceSql = """
                SELECT 
                    COUNT(*) as processed_records,
                    COUNT(DISTINCT employee_id) as processed_employees,
                    COUNT(DISTINCT device_serial) as processed_terminals,
                    SUM(CASE WHEN punch_in_time IS NOT NULL THEN 1 ELSE 0 END) as check_ins,
                    SUM(CASE WHEN punch_out_time IS NOT NULL THEN 1 ELSE 0 END) as check_outs
                FROM new_hrm.attendance 
                WHERE date = ? AND attendance_source = 'BIOMETRIC'
                """;

            Map<String, Object> attendanceStats = jdbcTemplate.queryForMap(attendanceSql, targetDate);

            // Processing efficiency
            Integer totalTransactions = (Integer) middlewareStats.get("total_transactions");
            Integer processedRecords = (Integer) attendanceStats.get("processed_records");
            double efficiency = totalTransactions > 0 ? (double) processedRecords / totalTransactions * 100 : 0;

            stats.put("date", targetDate);
            stats.put("middlewareStats", middlewareStats);
            stats.put("attendanceStats", attendanceStats);
            stats.put("processingEfficiency", Math.round(efficiency * 100.0) / 100.0);
            stats.put("dataFlowHealthy", efficiency > 80);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting flow stats: " + e.getMessage());
        }
    }

    /**
     * Get configuration details
     */
    @GetMapping("/config")
    public ResponseEntity<?> getConfiguration() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // EasyTimePro configuration
            Map<String, Object> easyTimePro = new HashMap<>();
            easyTimePro.put("syncEnabled", easyTimeProConfig.getSync().isEnabled());
            easyTimePro.put("syncInterval", easyTimeProConfig.getSync().getInterval());
            easyTimePro.put("databases", easyTimeProConfig.getSync().getDatabases());
            easyTimePro.put("middlewareEnabled", easyTimeProConfig.getMiddleware().isEnabled());
            easyTimePro.put("sourceDatabase", easyTimeProConfig.getMiddleware().getSourceDatabase());
            easyTimePro.put("targetDatabase", easyTimeProConfig.getMiddleware().getTargetDatabase());
            easyTimePro.put("transactionTable", easyTimeProConfig.getMiddleware().getTransactionTable());
            easyTimePro.put("autoSync", easyTimeProConfig.getMiddleware().isAutoSync());
            easyTimePro.put("syncInterval", easyTimeProConfig.getMiddleware().getSyncInterval());

            // Biometric configuration
            Map<String, Object> biometric = new HashMap<>();
            biometric.put("realtimeEnabled", biometricConfig.getRealtime().isEnabled());
            biometric.put("tcpPort", biometricConfig.getRealtime().getTcpPort());
            biometric.put("processingEnabled", biometricConfig.getRealtime().isProcessingEnabled());
            biometric.put("duplicateDetection", biometricConfig.getRealtime().isDuplicateDetection());
            biometric.put("duplicateWindowMinutes", biometricConfig.getRealtime().getDuplicateWindowMinutes());
            biometric.put("storeRawData", biometricConfig.getStorage().isStoreRawData());
            biometric.put("storeTemplates", biometricConfig.getStorage().isStoreTemplates());
            biometric.put("verifyTypes", biometricConfig.getStorage().getVerifyTypes());

            config.put("easyTimePro", easyTimePro);
            config.put("biometric", biometric);

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting configuration: " + e.getMessage());
        }
    }
}
