package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.EnhancedBiometricSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/enhanced-biometric")
@CrossOrigin(origins = "*")
public class EnhancedBiometricController {

    @Autowired
    private EnhancedBiometricSyncService syncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Trigger manual sync with enhanced logging
     */
    @PostMapping("/sync-now")
    public ResponseEntity<?> triggerEnhancedSync() {
        try {
            Map<String, Object> result = syncService.triggerManualSync();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get comprehensive sync statistics
     */
    @GetMapping("/sync-stats")
    public ResponseEntity<?> getSyncStatistics() {
        try {
            Map<String, Object> stats = syncService.getSyncStatistics();
            
            // Add database statistics
            addDatabaseStatistics(stats);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get real-time data flow status with device breakdown
     */
    @GetMapping("/realtime-status")
    public ResponseEntity<?> getRealtimeStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // Get device-wise transaction counts
            String deviceStatsSql = """
                SELECT 
                    terminal_sn,
                    COUNT(*) as total_transactions,
                    COUNT(CASE WHEN DATE(punch_time) = CURDATE() THEN 1 END) as today_transactions,
                    COUNT(CASE WHEN punch_time >= NOW() - INTERVAL 1 HOUR THEN 1 END) as last_hour_transactions,
                    MIN(punch_time) as first_transaction,
                    MAX(punch_time) as last_transaction
                FROM easywdms.iclock_transaction 
                GROUP BY terminal_sn
                ORDER BY last_transaction DESC
                """;

            List<Map<String, Object>> deviceStats = jdbcTemplate.queryForList(deviceStatsSql);

            // Get subadmin-wise processing stats
            String subadminStatsSql = """
                SELECT 
                    s.id as subadmin_id,
                    s.companyName as subadmin_name,
                    COUNT(DISTINCT st.terminal_serial) as device_count,
                    COUNT(DISTINCT e.empId) as employee_count,
                    COUNT(CASE WHEN a.date = CURDATE() AND a.attendance_source = 'BIOMETRIC' THEN 1 END) as today_attendance
                FROM new_hrm.subadmin s
                LEFT JOIN new_hrm.subadmin_terminals st ON s.id = st.subadmin_id
                LEFT JOIN new_hrm.employee e ON s.id = e.subadmin_id
                LEFT JOIN new_hrm.attendance a ON e.empId = a.employee_id
                GROUP BY s.id, s.companyName
                ORDER BY today_attendance DESC
                """;

            List<Map<String, Object>> subadminStats = jdbcTemplate.queryForList(subadminStatsSql);

            // Get recent transactions with employee mapping
            String recentTransactionsSql = """
                SELECT 
                    it.id,
                    it.emp_code,
                    it.emp_id as machine_emp_id,
                    it.punch_time,
                    it.punch_state,
                    it.terminal_sn,
                    st.subadmin_id,
                    s.companyName as subadmin_name,
                    e.empId as hrm_emp_id,
                    e.fullName as employee_name,
                    CASE 
                        WHEN e.empId IS NOT NULL THEN 'MAPPED'
                        ELSE 'UNMAPPED'
                    END as mapping_status
                FROM easywdms.iclock_transaction it
                LEFT JOIN new_hrm.subadmin_terminals st ON it.terminal_sn = st.terminal_serial
                LEFT JOIN new_hrm.subadmin s ON st.subadmin_id = s.id
                LEFT JOIN new_hrm.employee_device_mapping edm ON (
                    (it.emp_code = edm.emp_code OR it.emp_id = edm.easytime_employee_id)
                    AND it.terminal_sn = edm.terminal_serial
                    AND st.subadmin_id = edm.subadmin_id
                )
                LEFT JOIN new_hrm.employee e ON edm.hrm_employee_id = e.empId
                WHERE it.punch_time >= NOW() - INTERVAL 2 HOUR
                ORDER BY it.punch_time DESC
                LIMIT 20
                """;

            List<Map<String, Object>> recentTransactions = jdbcTemplate.queryForList(recentTransactionsSql);

            status.put("deviceStats", deviceStats);
            status.put("subadminStats", subadminStats);
            status.put("recentTransactions", recentTransactions);
            status.put("lastUpdated", LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get unmapped employees (transactions without HRM mapping)
     */
    @GetMapping("/unmapped-employees")
    public ResponseEntity<?> getUnmappedEmployees() {
        try {
            String sql = """
                SELECT 
                    it.emp_code,
                    it.emp_id as machine_emp_id,
                    it.terminal_sn,
                    st.subadmin_id,
                    s.companyName as subadmin_name,
                    COUNT(*) as transaction_count,
                    MIN(it.punch_time) as first_seen,
                    MAX(it.punch_time) as last_seen
                FROM easywdms.iclock_transaction it
                LEFT JOIN new_hrm.subadmin_terminals st ON it.terminal_sn = st.terminal_serial
                LEFT JOIN new_hrm.subadmin s ON st.subadmin_id = s.id
                LEFT JOIN new_hrm.employee_device_mapping edm ON (
                    (it.emp_code = edm.emp_code OR it.emp_id = edm.easytime_employee_id)
                    AND it.terminal_sn = edm.terminal_serial
                    AND st.subadmin_id = edm.subadmin_id
                )
                WHERE edm.id IS NULL
                AND it.punch_time >= CURDATE() - INTERVAL 7 DAYS
                GROUP BY it.emp_code, it.emp_id, it.terminal_sn, st.subadmin_id, s.companyName
                ORDER BY transaction_count DESC, last_seen DESC
                """;

            List<Map<String, Object>> unmappedEmployees = jdbcTemplate.queryForList(sql);

            Map<String, Object> result = new HashMap<>();
            result.put("unmappedEmployees", unmappedEmployees);
            result.put("count", unmappedEmployees.size());
            result.put("lastChecked", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Create employee mapping for unmapped biometric data
     */
    @PostMapping("/create-mapping")
    public ResponseEntity<?> createEmployeeMapping(@RequestBody Map<String, Object> request) {
        try {
            Integer hrmEmployeeId = (Integer) request.get("hrmEmployeeId");
            String empCode = (String) request.get("empCode");
            Integer machineEmpId = (Integer) request.get("machineEmpId");
            String terminalSerial = (String) request.get("terminalSerial");
            Integer subadminId = (Integer) request.get("subadminId");

            if (hrmEmployeeId == null || terminalSerial == null || subadminId == null) {
                return ResponseEntity.badRequest().body("hrmEmployeeId, terminalSerial, and subadminId are required");
            }

            // Verify employee exists and belongs to subadmin
            String empCheckSql = "SELECT fullName FROM new_hrm.employee WHERE empId = ? AND subadmin_id = ?";
            String employeeName;
            try {
                employeeName = jdbcTemplate.queryForObject(empCheckSql, String.class, hrmEmployeeId, subadminId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Employee not found or doesn't belong to specified subadmin");
            }

            // Create mapping
            String insertSql = """
                INSERT INTO new_hrm.employee_device_mapping 
                (hrm_employee_id, subadmin_id, easytime_employee_id, terminal_serial, emp_code, 
                 fingerprint_enrolled, enrollment_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, true, 'COMPLETED', NOW(), NOW())
                """;

            jdbcTemplate.update(insertSql, hrmEmployeeId, subadminId, 
                              machineEmpId != null ? machineEmpId : hrmEmployeeId, 
                              terminalSerial, empCode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Employee mapping created successfully");
            result.put("employeeName", employeeName);
            result.put("hrmEmployeeId", hrmEmployeeId);
            result.put("empCode", empCode);
            result.put("machineEmpId", machineEmpId);
            result.put("terminalSerial", terminalSerial);
            result.put("subadminId", subadminId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get device configuration and status
     */
    @GetMapping("/device-status")
    public ResponseEntity<?> getDeviceStatus() {
        try {
            String sql = """
                SELECT 
                    st.terminal_serial,
                    st.terminal_name,
                    st.subadmin_id,
                    s.companyName as subadmin_name,
                    st.status,
                    st.location,
                    COUNT(DISTINCT edm.hrm_employee_id) as mapped_employees,
                    COUNT(CASE WHEN it.punch_time >= CURDATE() THEN 1 END) as today_transactions,
                    MAX(it.punch_time) as last_transaction
                FROM new_hrm.subadmin_terminals st
                LEFT JOIN new_hrm.subadmin s ON st.subadmin_id = s.id
                LEFT JOIN new_hrm.employee_device_mapping edm ON st.terminal_serial = edm.terminal_serial
                LEFT JOIN easywdms.iclock_transaction it ON st.terminal_serial = it.terminal_sn
                GROUP BY st.terminal_serial, st.terminal_name, st.subadmin_id, s.companyName, st.status, st.location
                ORDER BY last_transaction DESC
                """;

            List<Map<String, Object>> deviceStatus = jdbcTemplate.queryForList(sql);

            Map<String, Object> result = new HashMap<>();
            result.put("devices", deviceStatus);
            result.put("totalDevices", deviceStatus.size());
            result.put("lastChecked", LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Add database statistics to sync stats
     */
    private void addDatabaseStatistics(Map<String, Object> stats) {
        try {
            // Total transactions
            Integer totalTransactions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM easywdms.iclock_transaction", Integer.class);
            
            // Today's transactions
            Integer todayTransactions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()", Integer.class);
            
            // Processed attendance
            Integer processedAttendance = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM new_hrm.attendance WHERE attendance_source = 'BIOMETRIC'", Integer.class);
            
            // Today's processed attendance
            Integer todayProcessedAttendance = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'", Integer.class);

            stats.put("totalTransactions", totalTransactions);
            stats.put("todayTransactions", todayTransactions);
            stats.put("processedAttendance", processedAttendance);
            stats.put("todayProcessedAttendance", todayProcessedAttendance);
            stats.put("processingEfficiency", todayTransactions > 0 ? 
                     (double) todayProcessedAttendance / todayTransactions * 100 : 0);

        } catch (Exception e) {
            System.err.println("Error adding database statistics: " + e.getMessage());
        }
    }
}
