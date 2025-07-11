package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.BiometricAttendanceProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/biometric-sync")
@CrossOrigin(origins = "*")
public class BiometricSyncController {

    @Autowired
    private BiometricAttendanceProcessor processor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Trigger manual sync of biometric data
     */
    @PostMapping("/sync-now")
    public ResponseEntity<?> triggerSync() {
        try {
            Map<String, Object> result = processor.triggerManualSync();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get real-time biometric data flow status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getRealtimeStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // Count today's transactions
            String transactionSql = """
                SELECT COUNT(*) FROM easywdms.iclock_transaction 
                WHERE DATE(punch_time) = CURDATE()
                """;
            Integer todayTransactions = jdbcTemplate.queryForObject(transactionSql, Integer.class);

            // Count processed transactions
            String processedSql = """
                SELECT COUNT(*) FROM easywdms.iclock_transaction 
                WHERE DATE(punch_time) = CURDATE() AND sync_status = 'PROCESSED'
                """;
            Integer processedTransactions = jdbcTemplate.queryForObject(processedSql, Integer.class);

            // Count today's attendance records
            String attendanceSql = """
                SELECT COUNT(*) FROM new_hrm.attendance 
                WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'
                """;
            Integer todayAttendance = jdbcTemplate.queryForObject(attendanceSql, Integer.class);

            // Get latest transactions
            String latestSql = """
                SELECT emp_code, punch_time, punch_state, terminal_sn, sync_status
                FROM easywdms.iclock_transaction 
                ORDER BY punch_time DESC 
                LIMIT 5
                """;
            List<Map<String, Object>> latestTransactions = jdbcTemplate.queryForList(latestSql);

            // Get latest attendance
            String latestAttendanceSql = """
                SELECT a.employee_id, e.fullName, a.date, a.punch_in_time, a.punch_out_time, a.created_at
                FROM new_hrm.attendance a
                JOIN new_hrm.employee e ON a.employee_id = e.empId
                WHERE a.attendance_source = 'BIOMETRIC'
                ORDER BY a.created_at DESC 
                LIMIT 5
                """;
            List<Map<String, Object>> latestAttendance = jdbcTemplate.queryForList(latestAttendanceSql);

            status.put("todayTransactions", todayTransactions);
            status.put("processedTransactions", processedTransactions);
            status.put("todayAttendance", todayAttendance);
            status.put("processingRate", todayTransactions > 0 ? (double) processedTransactions / todayTransactions * 100 : 0);
            status.put("latestTransactions", latestTransactions);
            status.put("latestAttendance", latestAttendance);
            status.put("lastChecked", LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get employee mapping status
     */
    @GetMapping("/employee-mapping-status")
    public ResponseEntity<?> getEmployeeMappingStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // Get unique emp_codes from transactions
            String empCodesSql = """
                SELECT DISTINCT emp_code, emp_id, COUNT(*) as transaction_count
                FROM easywdms.iclock_transaction 
                WHERE DATE(punch_time) = CURDATE()
                GROUP BY emp_code, emp_id
                ORDER BY transaction_count DESC
                """;
            List<Map<String, Object>> empCodes = jdbcTemplate.queryForList(empCodesSql);

            // Check which emp_codes have mappings
            List<Map<String, Object>> mappingStatus = new ArrayList<>();
            for (Map<String, Object> empCodeData : empCodes) {
                String empCode = (String) empCodeData.get("emp_code");
                Integer machineEmpId = (Integer) empCodeData.get("emp_id");
                
                Map<String, Object> empStatus = new HashMap<>();
                empStatus.put("emp_code", empCode);
                empStatus.put("machine_emp_id", machineEmpId);
                empStatus.put("transaction_count", empCodeData.get("transaction_count"));

                // Check if mapping exists
                if (empCode != null) {
                    String mappingSql = "SELECT COUNT(*) FROM new_hrm.employee_device_mapping WHERE emp_code = ?";
                    Integer mappingCount = jdbcTemplate.queryForObject(mappingSql, Integer.class, empCode);
                    empStatus.put("has_mapping", mappingCount > 0);
                }

                // Check if employee exists by machine emp_id
                if (machineEmpId != null) {
                    String empSql = "SELECT fullName FROM new_hrm.employee WHERE empId = ?";
                    try {
                        String empName = jdbcTemplate.queryForObject(empSql, String.class, machineEmpId);
                        empStatus.put("employee_found", true);
                        empStatus.put("employee_name", empName);
                    } catch (Exception e) {
                        empStatus.put("employee_found", false);
                    }
                }

                mappingStatus.add(empStatus);
            }

            status.put("employeeMappingStatus", mappingStatus);
            status.put("totalUniqueEmployees", empCodes.size());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Create employee mapping for biometric integration
     */
    @PostMapping("/create-mapping")
    public ResponseEntity<?> createEmployeeMapping(@RequestBody Map<String, Object> request) {
        try {
            Integer hrmEmployeeId = (Integer) request.get("hrmEmployeeId");
            String empCode = (String) request.get("empCode");
            String terminalSerial = (String) request.get("terminalSerial");
            Integer subadminId = (Integer) request.get("subadminId");

            if (hrmEmployeeId == null || empCode == null || terminalSerial == null) {
                return ResponseEntity.badRequest().body("hrmEmployeeId, empCode, and terminalSerial are required");
            }

            // Check if employee exists
            String empCheckSql = "SELECT fullName FROM new_hrm.employee WHERE empId = ?";
            String employeeName;
            try {
                employeeName = jdbcTemplate.queryForObject(empCheckSql, String.class, hrmEmployeeId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Employee not found with ID: " + hrmEmployeeId);
            }

            // Create mapping
            String insertSql = """
                INSERT INTO new_hrm.employee_device_mapping 
                (hrm_employee_id, subadmin_id, easytime_employee_id, terminal_serial, emp_code, 
                 fingerprint_enrolled, enrollment_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, true, 'COMPLETED', NOW(), NOW())
                """;

            jdbcTemplate.update(insertSql, hrmEmployeeId, subadminId != null ? subadminId : 1, 
                              hrmEmployeeId, terminalSerial, empCode);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Employee mapping created successfully");
            result.put("employeeName", employeeName);
            result.put("empCode", empCode);
            result.put("terminalSerial", terminalSerial);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Test specific employee lookup
     */
    @GetMapping("/test-employee-lookup")
    public ResponseEntity<?> testEmployeeLookup(
            @RequestParam(required = false) String empCode,
            @RequestParam(required = false) Integer machineEmpId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("empCode", empCode);
            result.put("machineEmpId", machineEmpId);

            List<Map<String, Object>> foundEmployees = new ArrayList<>();

            // Test mapping lookup
            if (empCode != null) {
                String mappingSql = """
                    SELECT edm.*, e.fullName, e.email 
                    FROM new_hrm.employee_device_mapping edm
                    JOIN new_hrm.employee e ON edm.hrm_employee_id = e.empId
                    WHERE edm.emp_code = ?
                    """;
                List<Map<String, Object>> mappings = jdbcTemplate.queryForList(mappingSql, empCode);
                if (!mappings.isEmpty()) {
                    foundEmployees.addAll(mappings);
                    result.put("foundViaMapping", true);
                }
            }

            // Test direct employee lookup
            if (machineEmpId != null) {
                String empSql = "SELECT * FROM new_hrm.employee WHERE empId = ?";
                List<Map<String, Object>> employees = jdbcTemplate.queryForList(empSql, machineEmpId);
                if (!employees.isEmpty()) {
                    foundEmployees.addAll(employees);
                    result.put("foundViaDirectId", true);
                }
            }

            result.put("foundEmployees", foundEmployees);
            result.put("totalFound", foundEmployees.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get unprocessed transactions
     */
    @GetMapping("/unprocessed-transactions")
    public ResponseEntity<?> getUnprocessedTransactions(@RequestParam(defaultValue = "10") int limit) {
        try {
            String sql = """
                SELECT 
                    id, emp_code, punch_time, punch_state, verify_type,
                    terminal_sn, emp_id, sync_status
                FROM easywdms.iclock_transaction 
                WHERE (sync_status IS NULL OR sync_status != 'PROCESSED')
                AND punch_time >= CURDATE() - INTERVAL 1 DAY
                ORDER BY punch_time DESC 
                LIMIT ?
                """;

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", transactions.size());
            result.put("transactions", transactions);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
