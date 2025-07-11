package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.EasyTimeProSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/easytimepro-sync")
@CrossOrigin(origins = "*")
public class EasyTimeProSyncController {

    @Autowired
    private EasyTimeProSyncService syncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Manual sync trigger
     */
    @PostMapping("/manual-sync")
    public ResponseEntity<?> manualSync() {
        try {
            Map<String, Object> result = syncService.manualSync();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error in manual sync: " + e.getMessage());
        }
    }

    /**
     * Get sync statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSyncStats() {
        try {
            Map<String, Object> stats = syncService.getSyncStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting stats: " + e.getMessage());
        }
    }

    /**
     * Get raw transaction data from EasyTimePro
     */
    @GetMapping("/raw-transactions")
    public ResponseEntity<?> getRawTransactions(
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "50") int limit) {
        
        try {
            String dbName = database != null ? database : "new_hrm";
            String start = startDate != null ? startDate : LocalDate.now().minusDays(7).toString();
            String end = endDate != null ? endDate : LocalDate.now().toString();

            String sql = String.format("""
                SELECT 
                    id, emp_code, punch_time, punch_state, verify_type, 
                    terminal_sn, terminal_alias, area_alias, source, 
                    upload_time, emp_id, terminal_id, company_id
                FROM %s.iclock_transaction 
                WHERE DATE(punch_time) BETWEEN ? AND ?
                ORDER BY punch_time DESC 
                LIMIT ?
                """, dbName);

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql, start, end, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("database", dbName);
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("totalRecords", transactions.size());
            response.put("transactions", transactions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching raw transactions: " + e.getMessage());
        }
    }

    /**
     * Get processed attendance data
     */
    @GetMapping("/processed-attendance")
    public ResponseEntity<?> getProcessedAttendance(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer empId) {
        
        try {
            String start = startDate != null ? startDate : LocalDate.now().minusDays(7).toString();
            String end = endDate != null ? endDate : LocalDate.now().toString();

            StringBuilder sql = new StringBuilder("""
                SELECT 
                    a.id, a.employee_id, e.fullName, a.date, 
                    a.punch_in_time, a.punch_out_time, a.attendance_source,
                    a.biometric_user_id, a.device_serial, a.verify_type,
                    a.punch_source, a.status, a.working_hours
                FROM new_hrm.attendance a
                JOIN new_hrm.employee e ON a.employee_id = e.empId
                WHERE a.date BETWEEN ? AND ? 
                AND a.attendance_source = 'BIOMETRIC'
                """);

            List<Object> params = new ArrayList<>();
            params.add(start);
            params.add(end);

            if (empId != null) {
                sql.append(" AND a.employee_id = ?");
                params.add(empId);
            }

            sql.append(" ORDER BY a.date DESC, a.punch_in_time DESC");

            List<Map<String, Object>> attendance = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            Map<String, Object> response = new HashMap<>();
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("empId", empId);
            response.put("totalRecords", attendance.size());
            response.put("attendance", attendance);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching processed attendance: " + e.getMessage());
        }
    }

    /**
     * Compare data between databases
     */
    @GetMapping("/compare-databases")
    public ResponseEntity<?> compareDatabases(@RequestParam(required = false) String date) {
        try {
            String targetDate = date != null ? date : LocalDate.now().toString();

            // Count records in both databases
            String newHrmSql = "SELECT COUNT(*) FROM new_hrm.iclock_transaction WHERE DATE(punch_time) = ?";
            String easywdmsSql = "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = ?";
            String attendanceSql = "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = ? AND attendance_source = 'BIOMETRIC'";

            Integer newHrmCount = jdbcTemplate.queryForObject(newHrmSql, Integer.class, targetDate);
            Integer easywdmsCount = jdbcTemplate.queryForObject(easywdmsSql, Integer.class, targetDate);
            Integer attendanceCount = jdbcTemplate.queryForObject(attendanceSql, Integer.class, targetDate);

            // Get sample records from each database
            String sampleSql = "SELECT emp_code, punch_time, punch_state, terminal_sn FROM %s.iclock_transaction WHERE DATE(punch_time) = ? LIMIT 5";
            
            List<Map<String, Object>> newHrmSample = jdbcTemplate.queryForList(
                String.format(sampleSql, "new_hrm"), targetDate);
            List<Map<String, Object>> easywdmsSample = jdbcTemplate.queryForList(
                String.format(sampleSql, "easywdms"), targetDate);

            Map<String, Object> comparison = new HashMap<>();
            comparison.put("date", targetDate);
            comparison.put("newHrmCount", newHrmCount);
            comparison.put("easywdmsCount", easywdmsCount);
            comparison.put("processedAttendanceCount", attendanceCount);
            comparison.put("newHrmSample", newHrmSample);
            comparison.put("easywdmsSample", easywdmsSample);
            comparison.put("syncEfficiency", attendanceCount > 0 ? 
                (double) attendanceCount / Math.max(newHrmCount, easywdmsCount) * 100 : 0);

            return ResponseEntity.ok(comparison);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error comparing databases: " + e.getMessage());
        }
    }

    /**
     * Sync specific date range
     */
    @PostMapping("/sync-date-range")
    public ResponseEntity<?> syncDateRange(@RequestBody Map<String, Object> request) {
        try {
            String startDate = (String) request.get("startDate");
            String endDate = (String) request.get("endDate");
            String database = (String) request.get("database");

            if (startDate == null || endDate == null) {
                return ResponseEntity.badRequest().body("startDate and endDate are required");
            }

            String dbName = database != null ? database : "new_hrm";

            String sql = String.format("""
                SELECT 
                    id, emp_code, punch_time, punch_state, verify_type, 
                    terminal_sn, terminal_alias, area_alias, source, 
                    upload_time, emp_id, terminal_id, company_id
                FROM %s.iclock_transaction 
                WHERE DATE(punch_time) BETWEEN ? AND ?
                ORDER BY punch_time ASC
                """, dbName);

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql, startDate, endDate);

            // Process each transaction (this would use the sync service logic)
            int processedCount = 0;
            List<String> errors = new ArrayList<>();

            for (Map<String, Object> transaction : transactions) {
                try {
                    // Here you would call the sync service to process each transaction
                    // For now, we'll just count them
                    processedCount++;
                } catch (Exception e) {
                    errors.add("Error processing transaction " + transaction.get("id") + ": " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("database", dbName);
            result.put("startDate", startDate);
            result.put("endDate", endDate);
            result.put("totalTransactions", transactions.size());
            result.put("processedCount", processedCount);
            result.put("errors", errors);
            result.put("syncTime", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error syncing date range: " + e.getMessage());
        }
    }

    /**
     * Get employee mapping status
     */
    @GetMapping("/mapping-status")
    public ResponseEntity<?> getMappingStatus() {
        try {
            // Get unique emp_codes from iclock_transaction
            String transactionSql = """
                SELECT DISTINCT emp_code, terminal_sn, COUNT(*) as punch_count
                FROM new_hrm.iclock_transaction 
                WHERE punch_time >= CURDATE() - INTERVAL 30 DAYS
                GROUP BY emp_code, terminal_sn
                ORDER BY punch_count DESC
                """;

            List<Map<String, Object>> transactionCodes = jdbcTemplate.queryForList(transactionSql);

            // Get existing mappings
            String mappingSql = """
                SELECT emp_code, terminal_serial, hrm_employee_id, fingerprint_enrolled
                FROM new_hrm.employee_device_mapping
                """;

            List<Map<String, Object>> mappings = jdbcTemplate.queryForList(mappingSql);

            // Create mapping status
            List<Map<String, Object>> mappingStatus = new ArrayList<>();
            
            for (Map<String, Object> transaction : transactionCodes) {
                String empCode = (String) transaction.get("emp_code");
                String terminalSn = (String) transaction.get("terminal_sn");
                
                boolean hasMappingRecord = mappings.stream().anyMatch(m -> 
                    empCode.equals(m.get("emp_code")) && terminalSn.equals(m.get("terminal_serial")));

                Map<String, Object> status = new HashMap<>();
                status.put("empCode", empCode);
                status.put("terminalSn", terminalSn);
                status.put("punchCount", transaction.get("punch_count"));
                status.put("hasMappingRecord", hasMappingRecord);
                status.put("needsMapping", !hasMappingRecord);
                
                mappingStatus.add(status);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalUniqueCodes", transactionCodes.size());
            response.put("totalMappings", mappings.size());
            response.put("mappingStatus", mappingStatus);
            response.put("unmappedCodes", mappingStatus.stream()
                .filter(m -> !(Boolean) m.get("hasMappingRecord"))
                .count());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error getting mapping status: " + e.getMessage());
        }
    }

    /**
     * Test database connections
     */
    @GetMapping("/test-connections")
    public ResponseEntity<?> testConnections() {
        try {
            Map<String, Object> connectionStatus = new HashMap<>();

            // Test new_hrm database
            try {
                Integer newHrmCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM new_hrm.iclock_transaction", Integer.class);
                connectionStatus.put("newHrmConnection", "OK");
                connectionStatus.put("newHrmRecords", newHrmCount);
            } catch (Exception e) {
                connectionStatus.put("newHrmConnection", "FAILED: " + e.getMessage());
            }

            // Test easywdms database
            try {
                Integer easywdmsCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM easywdms.iclock_transaction", Integer.class);
                connectionStatus.put("easywdmsConnection", "OK");
                connectionStatus.put("easywdmsRecords", easywdmsCount);
            } catch (Exception e) {
                connectionStatus.put("easywdmsConnection", "FAILED: " + e.getMessage());
            }

            // Test attendance table
            try {
                Integer attendanceCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM new_hrm.attendance WHERE attendance_source = 'BIOMETRIC'", Integer.class);
                connectionStatus.put("attendanceConnection", "OK");
                connectionStatus.put("biometricAttendanceRecords", attendanceCount);
            } catch (Exception e) {
                connectionStatus.put("attendanceConnection", "FAILED: " + e.getMessage());
            }

            connectionStatus.put("testTime", LocalDateTime.now());

            return ResponseEntity.ok(connectionStatus);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error testing connections: " + e.getMessage());
        }
    }
}
