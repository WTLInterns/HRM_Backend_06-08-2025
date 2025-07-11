package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SimpleBiometricTestService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    /**
     * Simple test to process iclock data
     */
    @Transactional
    public Map<String, Object> testIclockProcessing() {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get sample iclock data
            String sql = """
                    SELECT
                        id, emp_code, emp_id, punch_time, punch_state, verify_type,
                        terminal_sn, terminal_alias, source
                    FROM easywdms.iclock_transaction
                    WHERE punch_time >= CURDATE() - INTERVAL 1 DAY
                    ORDER BY punch_time DESC
                    LIMIT 10
                    """;

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql);
            result.put("sampleTransactions", transactions);

            int processedCount = 0;
            List<Map<String, Object>> processedRecords = new ArrayList<>();

            for (Map<String, Object> transaction : transactions) {
                try {
                    Map<String, Object> processResult = processSimpleTransaction(transaction);
                    if ((Boolean) processResult.get("success")) {
                        processedCount++;
                    }
                    processedRecords.add(processResult);
                } catch (Exception e) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("transactionId", transaction.get("id"));
                    errorResult.put("error", e.getMessage());
                    processedRecords.add(errorResult);
                }
            }

            result.put("totalTransactions", transactions.size());
            result.put("processedCount", processedCount);
            result.put("processedRecords", processedRecords);
            result.put("testTime", LocalDateTime.now());

            return result;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * Process a single transaction with simple logic
     */
    @Transactional
    private Map<String, Object> processSimpleTransaction(Map<String, Object> transaction) {
        Map<String, Object> result = new HashMap<>();

        try {
            String empCode = (String) transaction.get("emp_code");
            String machineEmpIdStr = (String) transaction.get("emp_id"); // Keep as string for large numbers
            Object punchTimeObj = transaction.get("punch_time");
            String punchStateStr = (String) transaction.get("punch_state");
            String terminalSn = (String) transaction.get("terminal_sn");

            // Safe parsing
            Integer punchState = safeParseInteger(punchStateStr);
            Integer machineEmpId = safeParseInteger(machineEmpIdStr);

            result.put("transactionId", transaction.get("id"));
            result.put("empCode", empCode);
            result.put("machineEmpId", machineEmpId);
            result.put("terminalSn", terminalSn);

            // Parse punch time
            LocalDateTime punchDateTime = parseDateTime(punchTimeObj);
            if (punchDateTime == null) {
                result.put("success", false);
                result.put("error", "Invalid punch time");
                return result;
            }

            // Find employee using simple strategies
            Employee employee = findEmployeeSimple(empCode, machineEmpId);
            if (employee == null) {
                result.put("success", false);
                result.put("error", "Employee not found");
                result.put("searchedEmpCode", empCode);
                result.put("searchedMachineEmpId", machineEmpId);
                return result;
            }

            result.put("foundEmployee", Map.of(
                    "empId", employee.getEmpId(),
                    "fullName", employee.getFullName(),
                    "subadminId", employee.getSubadmin() != null ? employee.getSubadmin().getId() : "null"));

            // Create or update attendance
            boolean attendanceCreated = createSimpleAttendance(employee, punchDateTime, punchState, transaction);

            result.put("success", attendanceCreated);
            result.put("attendanceCreated", attendanceCreated);
            result.put("punchTime", punchDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("punchType", (punchState != null && punchState == 0) ? "IN" : "OUT");

            return result;

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Simple employee finding logic
     */
    private Employee findEmployeeSimple(String empCode, Integer machineEmpId) {
        try {
            // Strategy 1: Try machine emp_id directly
            if (machineEmpId != null) {
                Optional<Employee> empOpt = employeeRepo.findById(machineEmpId);
                if (empOpt.isPresent()) {
                    System.out.println("✅ Found employee by machine emp_id: " + empOpt.get().getFullName());
                    return empOpt.get();
                }
            }

            // Strategy 2: Parse emp_code for employee ID
            if (empCode != null && empCode.contains("_EMP")) {
                Integer parsedEmpId = parseEmployeeIdFromCode(empCode);
                if (parsedEmpId != null) {
                    Optional<Employee> empOpt = employeeRepo.findById(parsedEmpId);
                    if (empOpt.isPresent()) {
                        System.out.println("✅ Found employee by parsing emp_code: " + empOpt.get().getFullName());
                        return empOpt.get();
                    }
                }
            }

            // Strategy 3: Try emp_code as direct employee ID
            if (empCode != null) {
                try {
                    Integer directEmpId = Integer.parseInt(empCode);
                    Optional<Employee> empOpt = employeeRepo.findById(directEmpId);
                    if (empOpt.isPresent()) {
                        System.out.println("✅ Found employee by direct emp_code: " + empOpt.get().getFullName());
                        return empOpt.get();
                    }
                } catch (NumberFormatException e) {
                    // Not a direct ID
                }
            }

            System.out.println("❌ No employee found for emp_code: " + empCode + ", machine_emp_id: " + machineEmpId);
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error finding employee: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse employee ID from emp_code
     */
    private Integer parseEmployeeIdFromCode(String empCode) {
        try {
            if (empCode.contains("_EMP")) {
                String[] parts = empCode.split("_");
                for (String part : parts) {
                    if (part.startsWith("EMP")) {
                        return Integer.parseInt(part.replace("EMP", ""));
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }
        return null;
    }

    /**
     * Create simple attendance record
     */
    @Transactional
    private boolean createSimpleAttendance(Employee employee, LocalDateTime punchDateTime,
            Integer punchState, Map<String, Object> transaction) {
        try {
            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();
            String dateStr = punchDate.toString();

            // Find or create attendance record
            Optional<Attendance> existingOpt = attendanceRepo.findByEmployeeAndDate(employee, dateStr);

            Attendance attendance;
            if (existingOpt.isPresent()) {
                attendance = existingOpt.get();
                System.out.println("📝 Updating existing attendance for " + employee.getFullName());
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
                System.out.println("📝 Creating new attendance for " + employee.getFullName());
            }

            // Set biometric fields
            attendance.setBiometricUserId(String.valueOf(transaction.get("emp_code")));
            attendance.setDeviceSerial((String) transaction.get("terminal_sn"));
            attendance.setVerifyType("fingerprint");
            attendance.setPunchSource("BIOMETRIC");
            attendance.setRawData(transaction.toString());

            // Set punch time based on state
            if (punchState != null && punchState == 0) {
                // Punch IN
                attendance.setPunchInTime(punchTime);
                attendance.setPunchIn(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                System.out.println("✅ Set punch IN: " + punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                // Punch OUT
                attendance.setPunchOutTime(punchTime);
                attendance.setPunchOut(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                System.out.println("✅ Set punch OUT: " + punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

            // Calculate working hours if both times are set
            if (attendance.getPunchInTime() != null && attendance.getPunchOutTime() != null) {
                attendance.calculateDurations();
            }

            // Save attendance
            attendanceRepo.save(attendance);
            System.out.println("💾 Saved attendance record for " + employee.getFullName());

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error creating attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Parse datetime from various formats
     */
    private LocalDateTime parseDateTime(Object dateTimeObj) {
        try {
            if (dateTimeObj instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateTimeObj).toLocalDateTime();
            } else if (dateTimeObj instanceof java.util.Date) {
                return LocalDateTime.ofInstant(((java.util.Date) dateTimeObj).toInstant(),
                        java.time.ZoneId.systemDefault());
            } else {
                String timeStr = dateTimeObj.toString();

                // Handle ISO format: 2025-07-11T12:36:22
                if (timeStr.contains("T")) {
                    timeStr = timeStr.replace("T", " ");
                }

                // Try different datetime formats
                DateTimeFormatter[] formatters = {
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                };

                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDateTime.parse(timeStr, formatter);
                    } catch (Exception ignored) {
                        // Try next formatter
                    }
                }

                // If all formatters fail, try parsing as ISO and converting
                return LocalDateTime.parse(dateTimeObj.toString().replace("T", " "));
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing datetime: " + dateTimeObj + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Get simple statistics
     */
    public Map<String, Object> getSimpleStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Count iclock transactions
            Integer iclockCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM easywdms.iclock_transaction WHERE DATE(punch_time) = CURDATE()",
                    Integer.class);

            // Count attendance records
            Integer attendanceCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM new_hrm.attendance WHERE date = CURDATE() AND attendance_source = 'BIOMETRIC'",
                    Integer.class);

            // Count employees
            Integer employeeCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM new_hrm.employee",
                    Integer.class);

            stats.put("todayIclockTransactions", iclockCount);
            stats.put("todayBiometricAttendance", attendanceCount);
            stats.put("totalEmployees", employeeCount);
            stats.put("processingRate", iclockCount > 0 ? (double) attendanceCount / iclockCount * 100 : 0);
            stats.put("lastChecked", LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * Safely parse String to Integer, handling large numbers and null values
     */
    private Integer safeParseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            // For very large numbers, just take the last few digits
            if (value.length() > 9) {
                // Take last 6 digits to avoid integer overflow
                String shortValue = value.substring(value.length() - 6);
                return Integer.parseInt(shortValue);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Could not parse integer from string: " + value);
            return null;
        }
    }
}
