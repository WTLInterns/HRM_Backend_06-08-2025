package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.EnhancedBiometricSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/iclock-verification")
@CrossOrigin(origins = "*")
public class IclockDataVerificationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EnhancedBiometricSyncService syncService;

    /**
     * Verify iclock_transaction table structure and data
     */
    @GetMapping("/verify-iclock-table")
    public ResponseEntity<?> verifyIclockTable() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Check if table exists
            String tableExistsSql = """
                SELECT COUNT(*) 
                FROM INFORMATION_SCHEMA.TABLES 
                WHERE TABLE_SCHEMA = 'easywdms' 
                AND TABLE_NAME = 'iclock_transaction'
                """;

            Integer tableExists = jdbcTemplate.queryForObject(tableExistsSql, Integer.class);
            result.put("tableExists", tableExists > 0);

            if (tableExists > 0) {
                // Get table structure
                String structureSql = """
                    SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
                    FROM INFORMATION_SCHEMA.COLUMNS 
                    WHERE TABLE_SCHEMA = 'easywdms' 
                    AND TABLE_NAME = 'iclock_transaction'
                    ORDER BY ORDINAL_POSITION
                    """;

                List<Map<String, Object>> tableStructure = jdbcTemplate.queryForList(structureSql);
                result.put("tableStructure", tableStructure);

                // Get record counts
                String countSql = "SELECT COUNT(*) FROM easywdms.iclock_transaction";
                Integer totalRecords = jdbcTemplate.queryForObject(countSql, Integer.class);
                result.put("totalRecords", totalRecords);

                // Get today's records
                String todayCountSql = "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()";
                Integer todayRecords = jdbcTemplate.queryForObject(todayCountSql, Integer.class);
                result.put("todayRecords", todayRecords);

                // Get recent records sample
                String sampleSql = """
                    SELECT id, emp_code, emp_id, punch_time, punch_state, verify_type, 
                           terminal_sn, terminal_alias, source, upload_time
                    FROM easywdms.iclock_transaction 
                    ORDER BY punch_time DESC 
                    LIMIT 10
                    """;

                List<Map<String, Object>> sampleRecords = jdbcTemplate.queryForList(sampleSql);
                result.put("sampleRecords", sampleRecords);

                // Get unique terminals
                String terminalsSql = """
                    SELECT terminal_sn, COUNT(*) as transaction_count,
                           MIN(punch_time) as first_transaction,
                           MAX(punch_time) as last_transaction
                    FROM easywdms.iclock_transaction 
                    GROUP BY terminal_sn
                    ORDER BY transaction_count DESC
                    """;

                List<Map<String, Object>> terminals = jdbcTemplate.queryForList(terminalsSql);
                result.put("terminals", terminals);

                // Get unique employee codes
                String empCodesSql = """
                    SELECT emp_code, emp_id, COUNT(*) as transaction_count,
                           MAX(punch_time) as last_punch
                    FROM easywdms.iclock_transaction 
                    WHERE emp_code IS NOT NULL
                    GROUP BY emp_code, emp_id
                    ORDER BY transaction_count DESC
                    LIMIT 20
                    """;

                List<Map<String, Object>> empCodes = jdbcTemplate.queryForList(empCodesSql);
                result.put("employeeCodes", empCodes);
            }

            result.put("verificationTime", LocalDateTime.now());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("message", "Error verifying iclock_transaction table");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Test data flow from iclock to HRM attendance
     */
    @PostMapping("/test-data-flow")
    public ResponseEntity<?> testDataFlow() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get current counts before sync
            Integer beforeIclockCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()", 
                Integer.class);

            Integer beforeAttendanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'", 
                Integer.class);

            result.put("beforeSync", Map.of(
                "iclockRecords", beforeIclockCount,
                "attendanceRecords", beforeAttendanceCount
            ));

            // Trigger manual sync
            Map<String, Object> syncResult = syncService.triggerManualSync();
            result.put("syncResult", syncResult);

            // Get counts after sync
            Integer afterIclockCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()", 
                Integer.class);

            Integer afterAttendanceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'", 
                Integer.class);

            result.put("afterSync", Map.of(
                "iclockRecords", afterIclockCount,
                "attendanceRecords", afterAttendanceCount,
                "newAttendanceRecords", afterAttendanceCount - beforeAttendanceCount
            ));

            // Get latest processed attendance records
            String latestAttendanceSql = """
                SELECT a.id, a.employee_id, e.fullName, a.date, 
                       a.punch_in_time, a.punch_out_time, a.attendance_source,
                       a.biometric_user_id, a.device_serial, a.verify_type,
                       a.created_at
                FROM new_hrm.attendance a
                JOIN new_hrm.employee e ON a.employee_id = e.empId
                WHERE a.attendance_source = 'BIOMETRIC'
                ORDER BY a.created_at DESC
                LIMIT 10
                """;

            List<Map<String, Object>> latestAttendance = jdbcTemplate.queryForList(latestAttendanceSql);
            result.put("latestProcessedAttendance", latestAttendance);

            result.put("testTime", LocalDateTime.now());
            result.put("dataFlowWorking", afterAttendanceCount > beforeAttendanceCount);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("message", "Error testing data flow");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Check employee mapping status for iclock data
     */
    @GetMapping("/mapping-status")
    public ResponseEntity<?> checkMappingStatus() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get iclock data with mapping status
            String mappingStatusSql = """
                SELECT 
                    it.emp_code,
                    it.emp_id as machine_emp_id,
                    it.terminal_sn,
                    COUNT(*) as transaction_count,
                    MAX(it.punch_time) as last_punch,
                    st.subadmin_id,
                    s.companyName as subadmin_name,
                    CASE 
                        WHEN edm.id IS NOT NULL THEN 'MAPPED'
                        ELSE 'UNMAPPED'
                    END as mapping_status,
                    e.empId as hrm_employee_id,
                    e.fullName as employee_name
                FROM easywdms.iclock_transaction it
                LEFT JOIN new_hrm.subadmin_terminals st ON it.terminal_sn = st.terminal_serial
                LEFT JOIN new_hrm.subadmin s ON st.subadmin_id = s.id
                LEFT JOIN new_hrm.employee_device_mapping edm ON (
                    (it.emp_code = edm.emp_code OR it.emp_id = edm.easytime_employee_id)
                    AND it.terminal_sn = edm.terminal_serial
                    AND st.subadmin_id = edm.subadmin_id
                )
                LEFT JOIN new_hrm.employee e ON edm.hrm_employee_id = e.empId
                WHERE it.punch_time >= CURDATE() - INTERVAL 7 DAYS
                GROUP BY it.emp_code, it.emp_id, it.terminal_sn, st.subadmin_id, 
                         s.companyName, edm.id, e.empId, e.fullName
                ORDER BY transaction_count DESC
                """;

            List<Map<String, Object>> mappingStatus = jdbcTemplate.queryForList(mappingStatusSql);
            result.put("mappingStatus", mappingStatus);

            // Count mapped vs unmapped
            long mappedCount = mappingStatus.stream()
                .filter(record -> "MAPPED".equals(record.get("mapping_status")))
                .count();

            long unmappedCount = mappingStatus.stream()
                .filter(record -> "UNMAPPED".equals(record.get("mapping_status")))
                .count();

            result.put("summary", Map.of(
                "totalEmployees", mappingStatus.size(),
                "mappedEmployees", mappedCount,
                "unmappedEmployees", unmappedCount,
                "mappingPercentage", mappingStatus.size() > 0 ? 
                    (double) mappedCount / mappingStatus.size() * 100 : 0
            ));

            result.put("checkTime", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("message", "Error checking mapping status");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Monitor real-time data flow
     */
    @GetMapping("/monitor-realtime")
    public ResponseEntity<?> monitorRealtimeFlow() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get last 1 hour activity
            String realtimeActivitySql = """
                SELECT 
                    DATE_FORMAT(punch_time, '%Y-%m-%d %H:%i:00') as time_bucket,
                    COUNT(*) as transaction_count,
                    COUNT(DISTINCT emp_code) as unique_employees,
                    COUNT(DISTINCT terminal_sn) as unique_devices
                FROM easywdms.iclock_transaction 
                WHERE punch_time >= NOW() - INTERVAL 1 HOUR
                GROUP BY DATE_FORMAT(punch_time, '%Y-%m-%d %H:%i:00')
                ORDER BY time_bucket DESC
                """;

            List<Map<String, Object>> realtimeActivity = jdbcTemplate.queryForList(realtimeActivitySql);
            result.put("realtimeActivity", realtimeActivity);

            // Get processing lag
            String processingLagSql = """
                SELECT 
                    AVG(TIMESTAMPDIFF(SECOND, it.punch_time, a.created_at)) as avg_processing_lag_seconds,
                    MIN(TIMESTAMPDIFF(SECOND, it.punch_time, a.created_at)) as min_processing_lag_seconds,
                    MAX(TIMESTAMPDIFF(SECOND, it.punch_time, a.created_at)) as max_processing_lag_seconds
                FROM easywdms.iclock_transaction it
                JOIN new_hrm.employee_device_mapping edm ON (
                    (it.emp_code = edm.emp_code OR it.emp_id = edm.easytime_employee_id)
                    AND it.terminal_sn = edm.terminal_serial
                )
                JOIN new_hrm.attendance a ON (
                    edm.hrm_employee_id = a.employee_id
                    AND DATE(it.punch_time) = STR_TO_DATE(a.date, '%Y-%m-%d')
                    AND a.attendance_source = 'BIOMETRIC'
                )
                WHERE it.punch_time >= NOW() - INTERVAL 1 HOUR
                """;

            try {
                Map<String, Object> processingLag = jdbcTemplate.queryForMap(processingLagSql);
                result.put("processingLag", processingLag);
            } catch (Exception e) {
                result.put("processingLag", "No recent processed data for lag calculation");
            }

            // Get current sync statistics
            Map<String, Object> syncStats = syncService.getSyncStatistics();
            result.put("syncStatistics", syncStats);

            result.put("monitorTime", LocalDateTime.now());
            result.put("isRealTimeActive", !realtimeActivity.isEmpty());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("message", "Error monitoring real-time flow");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Create sample employee mapping for testing
     */
    @PostMapping("/create-sample-mapping")
    public ResponseEntity<?> createSampleMapping(@RequestBody Map<String, Object> request) {
        try {
            String empCode = (String) request.get("empCode");
            Integer machineEmpId = (Integer) request.get("machineEmpId");
            String terminalSerial = (String) request.get("terminalSerial");
            Integer hrmEmployeeId = (Integer) request.get("hrmEmployeeId");
            Integer subadminId = (Integer) request.get("subadminId");

            if (empCode == null || terminalSerial == null || hrmEmployeeId == null || subadminId == null) {
                return ResponseEntity.badRequest().body("empCode, terminalSerial, hrmEmployeeId, and subadminId are required");
            }

            // Verify employee exists
            String empCheckSql = "SELECT fullName FROM new_hrm.employee WHERE empId = ? AND subadmin_id = ?";
            String employeeName = jdbcTemplate.queryForObject(empCheckSql, String.class, hrmEmployeeId, subadminId);

            // Create mapping
            String insertSql = """
                INSERT INTO new_hrm.employee_device_mapping 
                (hrm_employee_id, subadmin_id, easytime_employee_id, terminal_serial, emp_code, 
                 fingerprint_enrolled, enrollment_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, true, 'COMPLETED', NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                emp_code = VALUES(emp_code),
                easytime_employee_id = VALUES(easytime_employee_id),
                updated_at = NOW()
                """;

            jdbcTemplate.update(insertSql, hrmEmployeeId, subadminId, 
                              machineEmpId != null ? machineEmpId : hrmEmployeeId, 
                              terminalSerial, empCode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Sample mapping created successfully");
            result.put("employeeName", employeeName);
            result.put("mapping", Map.of(
                "hrmEmployeeId", hrmEmployeeId,
                "empCode", empCode,
                "machineEmpId", machineEmpId,
                "terminalSerial", terminalSerial,
                "subadminId", subadminId
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
