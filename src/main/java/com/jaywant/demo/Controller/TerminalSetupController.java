package com.jaywant.demo.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/terminal-setup")
@CrossOrigin(origins = "*")
public class TerminalSetupController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Register your F22 device in the system
     */
    @PostMapping("/register-f22-device")
    public ResponseEntity<?> registerF22Device() {
        try {
            String terminalSerial = "BOCK194960340";

            // Check if terminal already exists
            String checkSql = "SELECT COUNT(*) FROM new_hrm.subadmin_terminals WHERE terminal_serial = ?";
            Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, terminalSerial);

            if (existingCount > 0) {
                // Update existing terminal to ACTIVE
                String updateSql = """
                        UPDATE new_hrm.subadmin_terminals
                        SET status = 'ACTIVE', updated_at = NOW()
                        WHERE terminal_serial = ?
                        """;
                jdbcTemplate.update(updateSql, terminalSerial);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "F22 device updated to ACTIVE status",
                        "terminalSerial", terminalSerial,
                        "action", "updated"));
            } else {
                // Insert new terminal record
                String insertSql = """
                        INSERT INTO new_hrm.subadmin_terminals
                        (subadmin_id, terminal_serial, terminal_name, location, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, NOW(), NOW())
                        """;

                jdbcTemplate.update(insertSql,
                        2, // Default subadmin ID - you can change this
                        terminalSerial,
                        "F22 Biometric Device",
                        "Office Main Entrance",
                        "ACTIVE");

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "F22 device registered successfully",
                        "terminalSerial", terminalSerial,
                        "subadminId", 2,
                        "action", "created"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Create employee mapping for testing
     */
    @PostMapping("/create-test-mapping")
    public ResponseEntity<?> createTestMapping(@RequestBody Map<String, Object> request) {
        try {
            String empCode = (String) request.get("empCode");
            Integer hrmEmployeeId = (Integer) request.get("hrmEmployeeId");
            Integer subadminId = (Integer) request.get("subadminId");
            String terminalSerial = "BOCK194960340";

            if (empCode == null || hrmEmployeeId == null || subadminId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "empCode, hrmEmployeeId, and subadminId are required"));
            }

            // Check if employee exists
            String empCheckSql = "SELECT fullName FROM new_hrm.employee WHERE empId = ? AND subadmin_id = ?";
            String employeeName;
            try {
                employeeName = jdbcTemplate.queryForObject(empCheckSql, String.class, hrmEmployeeId, subadminId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Employee not found with ID " + hrmEmployeeId + " in subadmin " + subadminId));
            }

            // Create or update mapping
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

            jdbcTemplate.update(insertSql,
                    hrmEmployeeId,
                    subadminId,
                    hrmEmployeeId, // Use HRM employee ID as machine ID for simplicity
                    terminalSerial,
                    empCode);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Employee mapping created successfully",
                    "employeeName", employeeName,
                    "empCode", empCode,
                    "hrmEmployeeId", hrmEmployeeId,
                    "terminalSerial", terminalSerial,
                    "subadminId", subadminId));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Get current terminal and mapping status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSetupStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // Check terminal registration
            String terminalSql = """
                    SELECT terminal_serial, terminal_name, subadmin_id, status, location
                    FROM new_hrm.subadmin_terminals
                    WHERE terminal_serial = 'BOCK194960340'
                    """;

            List<Map<String, Object>> terminals = jdbcTemplate.queryForList(terminalSql);
            status.put("terminalRegistered", !terminals.isEmpty());
            status.put("terminalDetails", terminals.isEmpty() ? null : terminals.get(0));

            // Check employee mappings (simplified query to avoid table structure issues)
            String mappingSql = """
                    SELECT COUNT(*) as mapping_count
                    FROM new_hrm.employee_device_mapping
                    WHERE terminal_serial = 'BOCK194960340'
                    """;

            try {
                Integer mappingCount = jdbcTemplate.queryForObject(mappingSql, Integer.class);
                status.put("mappedEmployeeCount", mappingCount);
                status.put("employeeMappings", "Found " + mappingCount + " employee mappings");
            } catch (Exception e) {
                status.put("mappedEmployeeCount", 0);
                status.put("employeeMappings", "Could not check mappings: " + e.getMessage());
            }

            // Check recent iclock data
            String iclockSql = """
                    SELECT emp_code, COUNT(*) as transaction_count, MAX(punch_time) as last_punch
                    FROM easywdms.iclock_transaction
                    WHERE terminal_sn = 'BOCK194960340'
                    AND punch_time >= CURDATE()
                    GROUP BY emp_code
                    """;

            List<Map<String, Object>> todayTransactions = jdbcTemplate.queryForList(iclockSql);
            status.put("todayTransactions", todayTransactions);

            // Check processed attendance
            String attendanceSql = """
                    SELECT COUNT(*) as processed_count
                    FROM new_hrm.attendance
                    WHERE device_serial = 'BOCK194960340'
                    AND date = CURDATE()
                    AND attendance_source = 'BIOMETRIC'
                    """;

            Integer processedCount = jdbcTemplate.queryForObject(attendanceSql, Integer.class);
            status.put("processedAttendanceToday", processedCount);

            // Check if setup is complete
            boolean hasTerminals = !terminals.isEmpty();
            Integer mappingCount = (Integer) status.get("mappedEmployeeCount");
            boolean hasMappings = mappingCount != null && mappingCount > 0;

            status.put("setupComplete", hasTerminals && hasMappings);
            status.put("lastChecked", LocalDateTime.now());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Quick setup for your specific scenario
     */
    @PostMapping("/quick-setup")
    public ResponseEntity<?> quickSetup() {
        try {
            Map<String, Object> result = new HashMap<>();
            List<String> steps = new ArrayList<>();

            // Step 1: Register terminal
            try {
                registerF22Device();
                steps.add("✅ Terminal BOCK194960340 registered");
            } catch (Exception e) {
                steps.add("❌ Terminal registration failed: " + e.getMessage());
            }

            // Step 2: Create sample mappings for existing emp_codes
            String[] empCodes = { "52", "552", "952" };
            Integer[] employeeIds = { 52, 552, 952 };
            Integer subadminId = 2;

            for (int i = 0; i < empCodes.length; i++) {
                try {
                    Map<String, Object> mappingRequest = Map.of(
                            "empCode", empCodes[i],
                            "hrmEmployeeId", employeeIds[i],
                            "subadminId", subadminId);
                    createTestMapping(mappingRequest);
                    steps.add("✅ Mapping created for emp_code: " + empCodes[i]);
                } catch (Exception e) {
                    steps.add("⚠️ Mapping for emp_code " + empCodes[i] + ": " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("message", "Quick setup completed");
            result.put("steps", steps);
            result.put("nextAction", "Test punch on F22 device and check processing");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }
}
