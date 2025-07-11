package com.jaywant.demo.Service;

import com.jaywant.demo.Config.EasyTimeProConfig;
import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Arrays;

@Service
public class EasyTimeProSyncService {

    @Autowired
    private EasyTimeProConfig easyTimeProConfig;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Set<String> processedTransactions = new HashSet<>();

    /**
     * Sync data from EasyTimePro iclock_transaction table (configurable interval)
     * DISABLED: Replaced by EnhancedBiometricSyncService
     */
    // @Scheduled(fixedRateString = "#{@easyTimeProConfig.middleware.syncInterval}")
    public void syncEasyTimeProData() {
        try {
            if (!easyTimeProConfig.getMiddleware().isEnabled() || !easyTimeProConfig.getMiddleware().isAutoSync()) {
                return;
            }

            System.out.println("🔄 Starting EasyTimePro data sync...");

            // Sync from configured databases
            String[] databases = easyTimeProConfig.getSync().getDatabases().split(",");
            for (String database : databases) {
                syncFromDatabase(database.trim(), easyTimeProConfig.getMiddleware().getTransactionTable());
            }

            System.out.println("✅ EasyTimePro data sync completed");

        } catch (Exception e) {
            System.err.println("❌ Error in EasyTimePro sync: " + e.getMessage());
        }
    }

    /**
     * Manual sync trigger
     */
    public Map<String, Object> manualSync() {
        try {
            System.out.println("🔄 Manual sync triggered...");
            int newRecords = 0;

            // Use configured databases
            String[] databases = easyTimeProConfig.getSync().getDatabases().split(",");
            String table = easyTimeProConfig.getMiddleware().getTransactionTable();

            for (String database : databases) {
                newRecords += syncFromDatabase(database.trim(), table);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Manual sync completed");
            result.put("newRecords", newRecords);
            result.put("syncTime", LocalDateTime.now());
            result.put("processedDatabases", Arrays.asList(databases));

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Sync failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return result;
        }
    }

    /**
     * Sync from specific database and table
     */
    private int syncFromDatabase(String database, String table) {
        try {
            // First check if database and table exist
            if (!checkDatabaseAccess(database, table)) {
                System.out.println("⚠️ Skipping " + database + "." + table + " - not accessible");
                return 0;
            }

            String sql = String.format("""
                    SELECT
                        id, emp_code, punch_time, punch_state, verify_type,
                        terminal_sn, terminal_alias, area_alias, source,
                        upload_time, emp_id, terminal_id, company_id
                    FROM %s.%s
                    WHERE punch_time >= CURDATE() - INTERVAL 7 DAYS
                    ORDER BY punch_time DESC
                    LIMIT 1000
                    """, database, table);

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql);

            int processedCount = 0;

            for (Map<String, Object> transaction : transactions) {
                if (processTransaction(transaction, database)) {
                    processedCount++;
                }
            }

            System.out.println("📊 Processed " + processedCount + " records from " + database + "." + table);
            return processedCount;

        } catch (Exception e) {
            System.err.println("❌ Error syncing from " + database + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Check if database and table are accessible
     */
    private boolean checkDatabaseAccess(String database, String table) {
        try {
            // Check if database exists
            String checkDbSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
            List<Map<String, Object>> dbResult = jdbcTemplate.queryForList(checkDbSql, database);

            if (dbResult.isEmpty()) {
                System.out.println("⚠️ Database '" + database + "' does not exist");
                return false;
            }

            // Check if table exists
            String checkTableSql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
            List<Map<String, Object>> tableResult = jdbcTemplate.queryForList(checkTableSql, database, table);

            if (tableResult.isEmpty()) {
                System.out.println("⚠️ Table '" + database + "." + table + "' does not exist");
                return false;
            }

            // Test basic access with minimal query
            String testSql = String.format("SELECT COUNT(*) FROM %s.%s LIMIT 1", database, table);
            jdbcTemplate.queryForObject(testSql, Integer.class);

            System.out.println("✅ Database access verified for " + database + "." + table);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Cannot access " + database + "." + table + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Process individual transaction record
     */
    @Transactional
    private boolean processTransaction(Map<String, Object> transaction, String sourceDb) {
        try {
            // Create unique transaction ID to avoid duplicates
            String transactionId = sourceDb + "_" + transaction.get("id") + "_" +
                    transaction.get("emp_code") + "_" + transaction.get("punch_time");

            if (processedTransactions.contains(transactionId)) {
                return false; // Already processed
            }

            // Extract transaction data
            String empCode = (String) transaction.get("emp_code");
            Object punchTimeObj = transaction.get("punch_time");
            Integer punchState = (Integer) transaction.get("punch_state");
            Integer verifyType = (Integer) transaction.get("verify_type");
            String terminalSn = (String) transaction.get("terminal_sn");

            if (empCode == null || punchTimeObj == null || terminalSn == null) {
                return false;
            }

            // Parse punch time
            LocalDateTime punchDateTime;
            if (punchTimeObj instanceof java.sql.Timestamp) {
                punchDateTime = ((java.sql.Timestamp) punchTimeObj).toLocalDateTime();
            } else {
                punchDateTime = LocalDateTime.parse(punchTimeObj.toString(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            // Find employee mapping
            EmployeeMapping mapping = findEmployeeByCode(empCode, terminalSn);
            if (mapping == null) {
                System.out.println(
                        "⚠️ No employee mapping found for emp_code: " + empCode + " on terminal: " + terminalSn);
                return false;
            }

            // Process attendance
            boolean success = processAttendanceFromTransaction(mapping, punchDateTime, punchState, verifyType,
                    transaction);

            if (success) {
                processedTransactions.add(transactionId);

                // Send real-time notification
                broadcastAttendanceUpdate(mapping.employee, punchDateTime, punchState);
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Error processing transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find employee by emp_code and terminal
     */
    private EmployeeMapping findEmployeeByCode(String empCode, String terminalSn) {
        try {
            System.out.println("🔍 Looking for employee with emp_code: " + empCode + " on terminal: " + terminalSn);

            // Method 1: Find by emp_code in mapping table
            Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo.findByEmpCode(empCode);
            if (mappingOpt.isPresent()) {
                EmployeeDeviceMapping mapping = mappingOpt.get();
                Employee employee = employeeRepo.findById(mapping.getHrmEmployeeId()).orElse(null);
                if (employee != null) {
                    System.out.println("✅ Found employee via mapping: " + employee.getFullName() + " (ID: "
                            + employee.getEmpId() + ")");
                    return new EmployeeMapping(employee, mapping);
                }
            }

            // Method 2: Parse emp_code format (SA{subadmin}_EMP{empId}_{terminal})
            if (empCode.contains("_EMP") && empCode.contains("_")) {
                String[] parts = empCode.split("_");
                if (parts.length >= 2) {
                    String empIdStr = parts[1].replace("EMP", "");
                    try {
                        Integer empId = Integer.parseInt(empIdStr);
                        Employee employee = employeeRepo.findById(empId).orElse(null);
                        if (employee != null) {
                            System.out.println("✅ Found employee via parsing: " + employee.getFullName() + " (ID: "
                                    + employee.getEmpId() + ")");

                            // Find existing mapping or create temporary one
                            EmployeeDeviceMapping mapping = mappingRepo
                                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSn)
                                    .orElse(null);

                            return new EmployeeMapping(employee, mapping);
                        } else {
                            System.out.println("⚠️ Employee ID " + empId + " not found in database");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("⚠️ Could not parse employee ID from: " + empIdStr);
                    }
                }
            }

            // Method 3: Try to find by emp_code directly (if it's just the employee ID)
            try {
                Integer directEmpId = Integer.parseInt(empCode);
                Employee employee = employeeRepo.findById(directEmpId).orElse(null);
                if (employee != null) {
                    System.out.println("✅ Found employee via direct ID: " + employee.getFullName() + " (ID: "
                            + employee.getEmpId() + ")");
                    return new EmployeeMapping(employee, null);
                }
            } catch (NumberFormatException e) {
                // Not a direct ID, continue
            }

            System.out.println("❌ No employee found for emp_code: " + empCode);
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error finding employee by code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Process attendance from transaction data
     */
    private boolean processAttendanceFromTransaction(EmployeeMapping mapping, LocalDateTime punchDateTime,
            Integer punchState, Integer verifyType,
            Map<String, Object> transaction) {
        try {
            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();
            String dateStr = punchDate.toString();

            // Find or create attendance record
            Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(mapping.employee, dateStr);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(mapping.employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Set biometric fields
            attendance.setPunchSource("BIOMETRIC");
            attendance.setBiometricUserId(String.valueOf(transaction.get("emp_code")));
            attendance.setDeviceSerial((String) transaction.get("terminal_sn"));
            attendance.setVerifyType(getVerifyTypeString(verifyType));
            attendance.setRawData(transaction.toString());

            // Determine punch type and set time
            String punchType = determinePunchType(attendance, punchTime, punchState);

            if ("check_in".equals(punchType)) {
                attendance.setPunchInTime(punchTime);
            } else if ("check_out".equals(punchType)) {
                attendance.setPunchOutTime(punchTime);
            }

            // Calculate durations
            attendance.calculateDurations();

            // Save attendance
            attendanceRepo.save(attendance);

            System.out.println("✅ EasyTimePro attendance processed: " + mapping.employee.getFullName() +
                    " " + punchType + " at " + punchTime);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error processing attendance: " + e.getMessage());
            return false;
        }
    }

    /**
     * Determine punch type based on existing data and punch state
     */
    private String determinePunchType(Attendance attendance, LocalTime punchTime, Integer punchState) {
        // If punch_state is provided, use it (0=in, 1=out in some systems, varies by
        // configuration)
        if (punchState != null) {
            if (punchState == 0) {
                return "check_in";
            } else if (punchState == 1) {
                return "check_out";
            }
        }

        // Smart logic based on existing punches
        if (attendance.getPunchInTime() == null) {
            return "check_in";
        } else if (attendance.getPunchOutTime() == null) {
            return "check_out";
        } else {
            // Both punches exist - determine based on time
            if (punchTime.isBefore(attendance.getPunchInTime())) {
                return "check_in"; // Earlier time, update check-in
            } else {
                return "check_out"; // Later time, update check-out
            }
        }
    }

    /**
     * Convert verify type integer to string
     */
    private String getVerifyTypeString(Integer verifyType) {
        if (verifyType == null)
            return "fingerprint";

        switch (verifyType) {
            case 1:
                return "fingerprint";
            case 2:
                return "face";
            case 3:
                return "password";
            case 4:
                return "card";
            case 15:
                return "palm";
            default:
                return "fingerprint";
        }
    }

    /**
     * Broadcast real-time attendance update
     */
    private void broadcastAttendanceUpdate(Employee employee, LocalDateTime punchDateTime, Integer punchState) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "ATTENDANCE_UPDATE");
            update.put("empId", employee.getEmpId());
            update.put("employeeName", employee.getFullName());
            update.put("punchTime", punchDateTime.toString());
            update.put("punchType", punchState == 0 ? "check_in" : "check_out");
            update.put("source", "EasyTimePro");
            update.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/attendance/" + employee.getSubadmin().getId(), update);

        } catch (Exception e) {
            System.err.println("❌ Error broadcasting update: " + e.getMessage());
        }
    }

    /**
     * Get sync statistics
     */
    public Map<String, Object> getSyncStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Count records in both databases
            String newHrmCount = "SELECT COUNT(*) FROM new_hrm.iclock_transaction WHERE punch_time >= CURDATE()";
            String easywdmsCount = "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE punch_time >= CURDATE()";

            Integer newHrmRecords = jdbcTemplate.queryForObject(newHrmCount, Integer.class);
            Integer easywdmsRecords = jdbcTemplate.queryForObject(easywdmsCount, Integer.class);

            // Count processed attendance records today
            String attendanceCount = "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'";
            Integer attendanceRecords = jdbcTemplate.queryForObject(attendanceCount, Integer.class);

            stats.put("newHrmTransactions", newHrmRecords);
            stats.put("easywdmsTransactions", easywdmsRecords);
            stats.put("processedAttendance", attendanceRecords);
            stats.put("processedTransactionIds", processedTransactions.size());
            stats.put("lastSyncTime", LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("error", "Error getting stats: " + e.getMessage());
            return stats;
        }
    }

    // Helper class
    private static class EmployeeMapping {
        Employee employee;
        EmployeeDeviceMapping mapping;

        EmployeeMapping(Employee employee, EmployeeDeviceMapping mapping) {
            this.employee = employee;
            this.mapping = mapping;
        }
    }
}
