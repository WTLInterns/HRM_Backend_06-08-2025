package com.jaywant.demo.Controller;

import com.jaywant.demo.Service.EasyTimeProSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/easytimepro-debug")
@CrossOrigin(origins = "*")
public class EasyTimeProDebugController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EasyTimeProSyncService syncService;

    /**
     * Get latest transactions from easywdms database
     */
    @GetMapping("/latest-transactions")
    public ResponseEntity<?> getLatestTransactions(@RequestParam(defaultValue = "10") int limit) {
        try {
            String sql = """
                SELECT 
                    id, emp_code, punch_time, punch_state, verify_type,
                    terminal_sn, terminal_alias, area_alias, source,
                    upload_time, emp_id, terminal_id, company_id
                FROM easywdms.iclock_transaction 
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

    /**
     * Get processed attendance records from new_hrm database
     */
    @GetMapping("/processed-attendance")
    public ResponseEntity<?> getProcessedAttendance(@RequestParam(defaultValue = "10") int limit) {
        try {
            String sql = """
                SELECT 
                    a.id, a.employee_id, e.fullName, a.date, 
                    a.punch_in_time, a.punch_out_time, a.status,
                    a.attendance_source, a.biometric_user_id, a.device_serial,
                    a.verify_type, a.raw_data, a.created_at
                FROM new_hrm.attendance a
                LEFT JOIN new_hrm.employee e ON a.employee_id = e.empId
                WHERE a.attendance_source = 'BIOMETRIC'
                ORDER BY a.created_at DESC 
                LIMIT ?
                """;

            List<Map<String, Object>> attendance = jdbcTemplate.queryForList(sql, limit);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", attendance.size());
            result.put("attendance", attendance);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Check employee mappings
     */
    @GetMapping("/employee-mappings")
    public ResponseEntity<?> getEmployeeMappings() {
        try {
            String sql = """
                SELECT 
                    edm.id, edm.hrm_employee_id, edm.emp_code, edm.terminal_serial,
                    edm.fingerprint_enrolled, e.fullName, e.email
                FROM new_hrm.employee_device_mapping edm
                LEFT JOIN new_hrm.employee e ON edm.hrm_employee_id = e.empId
                ORDER BY edm.created_at DESC
                """;

            List<Map<String, Object>> mappings = jdbcTemplate.queryForList(sql);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", mappings.size());
            result.put("mappings", mappings);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Test employee lookup by emp_code
     */
    @GetMapping("/test-employee-lookup")
    public ResponseEntity<?> testEmployeeLookup(@RequestParam String empCode) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("empCode", empCode);

            // Check if emp_code exists in mapping table
            String mappingSql = "SELECT * FROM new_hrm.employee_device_mapping WHERE emp_code = ?";
            List<Map<String, Object>> mappings = jdbcTemplate.queryForList(mappingSql, empCode);
            result.put("mappingFound", !mappings.isEmpty());
            result.put("mappings", mappings);

            // Try to parse emp_code for employee ID
            if (empCode.contains("_EMP") && empCode.contains("_")) {
                String[] parts = empCode.split("_");
                if (parts.length >= 2) {
                    String empIdStr = parts[1].replace("EMP", "");
                    try {
                        Integer empId = Integer.parseInt(empIdStr);
                        
                        // Check if employee exists
                        String empSql = "SELECT * FROM new_hrm.employee WHERE empId = ?";
                        List<Map<String, Object>> employees = jdbcTemplate.queryForList(empSql, empId);
                        result.put("employeeFound", !employees.isEmpty());
                        result.put("employees", employees);
                        result.put("parsedEmpId", empId);
                        
                    } catch (NumberFormatException e) {
                        result.put("parseError", "Could not parse employee ID from: " + empIdStr);
                    }
                }
            }

            // Try direct ID lookup
            try {
                Integer directEmpId = Integer.parseInt(empCode);
                String empSql = "SELECT * FROM new_hrm.employee WHERE empId = ?";
                List<Map<String, Object>> employees = jdbcTemplate.queryForList(empSql, directEmpId);
                result.put("directIdFound", !employees.isEmpty());
                result.put("directEmployees", employees);
            } catch (NumberFormatException e) {
                result.put("directIdError", "emp_code is not a direct employee ID");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Process a specific transaction manually for testing
     */
    @PostMapping("/process-transaction")
    public ResponseEntity<?> processSpecificTransaction(@RequestParam Long transactionId) {
        try {
            // Get the specific transaction
            String sql = """
                SELECT 
                    id, emp_code, punch_time, punch_state, verify_type,
                    terminal_sn, terminal_alias, area_alias, source,
                    upload_time, emp_id, terminal_id, company_id
                FROM easywdms.iclock_transaction 
                WHERE id = ?
                """;

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql, transactionId);

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("Transaction not found with ID: " + transactionId);
            }

            Map<String, Object> transaction = transactions.get(0);

            // Trigger manual sync to process this transaction
            Map<String, Object> syncResult = syncService.manualSync();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("transaction", transaction);
            result.put("syncResult", syncResult);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get data flow comparison
     */
    @GetMapping("/data-flow-comparison")
    public ResponseEntity<?> getDataFlowComparison() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Count transactions in easywdms
            String transactionCountSql = "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()";
            Integer transactionCount = jdbcTemplate.queryForObject(transactionCountSql, Integer.class);

            // Count processed attendance in new_hrm
            String attendanceCountSql = "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'";
            Integer attendanceCount = jdbcTemplate.queryForObject(attendanceCountSql, Integer.class);

            // Get unique emp_codes from transactions
            String uniqueEmpCodesSql = "SELECT DISTINCT emp_code FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()";
            List<Map<String, Object>> uniqueEmpCodes = jdbcTemplate.queryForList(uniqueEmpCodesSql);

            // Get processed employees
            String processedEmpSql = "SELECT DISTINCT employee_id FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'";
            List<Map<String, Object>> processedEmployees = jdbcTemplate.queryForList(processedEmpSql);

            result.put("todayTransactions", transactionCount);
            result.put("todayProcessedAttendance", attendanceCount);
            result.put("uniqueEmpCodes", uniqueEmpCodes);
            result.put("processedEmployees", processedEmployees);
            result.put("processingRate", transactionCount > 0 ? (double) attendanceCount / transactionCount * 100 : 0);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
