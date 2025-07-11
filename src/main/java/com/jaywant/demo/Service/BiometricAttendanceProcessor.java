package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
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

@Service
public class BiometricAttendanceProcessor {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private Set<String> processedTransactions = new HashSet<>();

    /**
     * Real-time sync every 30 seconds
     * DISABLED: Replaced by EnhancedBiometricSyncService
     */
    // @Scheduled(fixedRate = 30000)
    public void processRealtimeBiometricData() {
        try {
            System.out.println("🔄 Processing real-time biometric data...");

            // Get unprocessed transactions from last 24 hours
            String sql = """
                    SELECT
                        id, emp_code, punch_time, punch_state, verify_type, work_code,
                        terminal_sn, terminal_alias, area_alias, longitude, latitude,
                        gps_location, mobile, source, purpose, crc, is_attendance,
                        reserved, upload_time, sync_status, sync_time, emp_id,
                        terminal_id, company_id, mask_flag, temperature
                    FROM easywdms.iclock_transaction
                    WHERE punch_time >= NOW() - INTERVAL 24 HOUR
                    AND (sync_status IS NULL OR sync_status != 'PROCESSED')
                    ORDER BY punch_time DESC
                    LIMIT 100
                    """;

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql);

            int processedCount = 0;
            for (Map<String, Object> transaction : transactions) {
                if (processTransaction(transaction)) {
                    processedCount++;
                    markTransactionAsProcessed(transaction);
                }
            }

            if (processedCount > 0) {
                System.out.println("✅ Processed " + processedCount + " biometric transactions");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in real-time biometric processing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process individual biometric transaction
     */
    @Transactional
    private boolean processTransaction(Map<String, Object> transaction) {
        try {
            // Create unique transaction ID
            String transactionId = "BIOMETRIC_" + transaction.get("id") + "_" +
                    transaction.get("emp_code") + "_" + transaction.get("punch_time");

            if (processedTransactions.contains(transactionId)) {
                return false; // Already processed
            }

            // Extract transaction data with safe type conversion
            String empCode = (String) transaction.get("emp_code");
            Object punchTimeObj = transaction.get("punch_time");
            Integer punchState = safeParseInteger(transaction.get("punch_state"));
            Integer verifyType = safeParseInteger(transaction.get("verify_type"));
            String terminalSn = (String) transaction.get("terminal_sn");
            Integer machineEmpId = safeParseInteger(transaction.get("emp_id"));

            if (empCode == null && machineEmpId == null) {
                System.out.println("⚠️ No employee identifier in transaction: " + transaction.get("id"));
                return false;
            }

            if (punchTimeObj == null) {
                System.out.println("⚠️ No punch time in transaction: " + transaction.get("id"));
                return false;
            }

            // Parse punch time
            LocalDateTime punchDateTime = parsePunchTime(punchTimeObj);
            if (punchDateTime == null) {
                return false;
            }

            // Find HRM employee
            Employee employee = findHrmEmployee(empCode, machineEmpId, terminalSn);
            if (employee == null) {
                System.out.println(
                        "⚠️ No HRM employee found for emp_code: " + empCode + ", machine_emp_id: " + machineEmpId);
                return false;
            }

            // Process attendance
            boolean success = processAttendanceRecord(employee, punchDateTime, punchState, verifyType, transaction);

            if (success) {
                processedTransactions.add(transactionId);
                System.out.println("✅ Processed attendance for: " + employee.getFullName() +
                        " at " + punchDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                // Send real-time notification
                sendRealtimeNotification(employee, punchDateTime, punchState);
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Error processing transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find HRM employee by various methods
     */
    private Employee findHrmEmployee(String empCode, Integer machineEmpId, String terminalSn) {
        try {
            // Method 1: Find by employee device mapping
            if (empCode != null) {
                Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo.findByEmpCode(empCode);
                if (mappingOpt.isPresent()) {
                    Employee employee = employeeRepo.findById(mappingOpt.get().getHrmEmployeeId()).orElse(null);
                    if (employee != null) {
                        System.out.println("✅ Found employee via mapping: " + employee.getFullName());
                        return employee;
                    }
                }
            }

            // Method 2: Parse emp_code format (SA{subadmin}_EMP{empId}_{terminal})
            if (empCode != null && empCode.contains("_EMP")) {
                String[] parts = empCode.split("_");
                for (String part : parts) {
                    if (part.startsWith("EMP")) {
                        try {
                            Integer empId = Integer.parseInt(part.replace("EMP", ""));
                            Employee employee = employeeRepo.findById(empId).orElse(null);
                            if (employee != null) {
                                System.out.println("✅ Found employee via emp_code parsing: " + employee.getFullName());
                                return employee;
                            }
                        } catch (NumberFormatException e) {
                            // Continue to next method
                        }
                    }
                }
            }

            // Method 3: Use machine employee ID directly
            if (machineEmpId != null) {
                Employee employee = employeeRepo.findById(machineEmpId).orElse(null);
                if (employee != null) {
                    System.out.println("✅ Found employee via machine emp_id: " + employee.getFullName());
                    return employee;
                }
            }

            // Method 4: Try emp_code as direct employee ID
            if (empCode != null) {
                try {
                    Integer directEmpId = Integer.parseInt(empCode);
                    Employee employee = employeeRepo.findById(directEmpId).orElse(null);
                    if (employee != null) {
                        System.out.println("✅ Found employee via direct emp_code: " + employee.getFullName());
                        return employee;
                    }
                } catch (NumberFormatException e) {
                    // Not a direct ID
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("❌ Error finding HRM employee: " + e.getMessage());
            return null;
        }
    }

    /**
     * Process attendance record
     */
    @Transactional
    private boolean processAttendanceRecord(Employee employee, LocalDateTime punchDateTime,
            Integer punchState, Integer verifyType,
            Map<String, Object> transaction) {
        try {
            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();
            String dateStr = punchDate.toString();

            // Find or create attendance record
            Optional<Attendance> existingOpt = attendanceRepo.findByEmployeeAndDate(employee, dateStr);

            Attendance attendance;
            if (existingOpt.isPresent()) {
                attendance = existingOpt.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Set biometric fields
            attendance.setBiometricUserId(String.valueOf(transaction.get("emp_code")));
            attendance.setDeviceSerial((String) transaction.get("terminal_sn"));
            attendance.setVerifyType(getVerifyTypeString(verifyType));
            attendance.setPunchSource("BIOMETRIC");
            attendance.setRawData(transaction.toString());

            // Determine punch type and set time
            String punchType = determinePunchType(attendance, punchTime, punchState);

            if ("punch_in".equals(punchType)) {
                attendance.setPunchInTime(punchTime);
                attendance.setPunchIn(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                System.out.println("✅ Set punch IN time: " + punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else if ("punch_out".equals(punchType)) {
                attendance.setPunchOutTime(punchTime);
                attendance.setPunchOut(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                System.out
                        .println("✅ Set punch OUT time: " + punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

            // Calculate working hours and durations
            attendance.calculateDurations();

            // Save attendance
            attendanceRepo.save(attendance);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error processing attendance record: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parse punch time from various formats
     */
    private LocalDateTime parsePunchTime(Object punchTimeObj) {
        try {
            if (punchTimeObj instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) punchTimeObj).toLocalDateTime();
            } else if (punchTimeObj instanceof java.util.Date) {
                return LocalDateTime.ofInstant(((java.util.Date) punchTimeObj).toInstant(),
                        java.time.ZoneId.systemDefault());
            } else {
                String timeStr = punchTimeObj.toString();
                return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing punch time: " + punchTimeObj + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Determine punch type (in/out) based on existing data and punch state
     */
    private String determinePunchType(Attendance attendance, LocalTime punchTime, Integer punchState) {
        // Use punch_state if available (0=in, 1=out in most systems)
        if (punchState != null) {
            return (punchState == 0) ? "punch_in" : "punch_out";
        }

        // Smart logic based on existing punches
        if (attendance.getPunchInTime() == null) {
            return "punch_in";
        } else if (attendance.getPunchOutTime() == null) {
            return "punch_out";
        } else {
            // Both exist - determine based on time
            return punchTime.isBefore(attendance.getPunchInTime()) ? "punch_in" : "punch_out";
        }
    }

    /**
     * Convert verify type to string
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
     * Mark transaction as processed
     */
    private void markTransactionAsProcessed(Map<String, Object> transaction) {
        try {
            String updateSql = "UPDATE easywdms.iclock_transaction SET sync_status = 'PROCESSED', sync_time = NOW() WHERE id = ?";
            jdbcTemplate.update(updateSql, transaction.get("id"));
        } catch (Exception e) {
            System.err.println("❌ Error marking transaction as processed: " + e.getMessage());
        }
    }

    /**
     * Send real-time notification
     */
    private void sendRealtimeNotification(Employee employee, LocalDateTime punchDateTime, Integer punchState) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "BIOMETRIC_ATTENDANCE");
            notification.put("employeeId", employee.getEmpId());
            notification.put("employeeName", employee.getFullName());
            notification.put("punchTime", punchDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            notification.put("punchType", (punchState != null && punchState == 0) ? "CHECK_IN" : "CHECK_OUT");
            notification.put("timestamp", LocalDateTime.now());

            messagingTemplate.convertAndSend("/topic/attendance", notification);
        } catch (Exception e) {
            System.err.println("❌ Error sending real-time notification: " + e.getMessage());
        }
    }

    /**
     * Manual sync trigger
     */
    public Map<String, Object> triggerManualSync() {
        try {
            processRealtimeBiometricData();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Manual biometric sync completed");
            result.put("timestamp", LocalDateTime.now());

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Manual sync failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Safely parse Object to Integer, handling String to Integer conversion
     */
    private Integer safeParseInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Could not parse integer from string: " + value);
                return null;
            }
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        System.err.println(
                "⚠️ Unexpected type for integer conversion: " + value.getClass().getSimpleName() + " = " + value);
        return null;
    }
}
