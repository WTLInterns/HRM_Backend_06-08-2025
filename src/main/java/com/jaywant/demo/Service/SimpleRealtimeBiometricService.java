package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimpleRealtimeBiometricService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    // Track processed transactions to avoid duplicates
    private final Set<Integer> processedTransactionIds = ConcurrentHashMap.newKeySet();
    
    // Last sync time
    private LocalDateTime lastSyncTime = LocalDateTime.now().minusHours(1);

    /**
     * Real-time sync every 15 seconds
     */
    @Scheduled(fixedRate = 15000) // 15 seconds
    public void performRealtimeSync() {
        try {
            System.out.println("🔄 [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] Simple real-time sync starting...");
            
            // Get new transactions since last sync
            String sql = """
                SELECT 
                    id, emp_code, emp_id, punch_time, punch_state, verify_type,
                    terminal_sn, terminal_alias, source
                FROM easywdms.iclock_transaction 
                WHERE punch_time > ?
                AND terminal_sn = 'BOCK194960340'
                ORDER BY punch_time ASC
                LIMIT 50
                """;

            List<Map<String, Object>> newTransactions = jdbcTemplate.queryForList(sql, lastSyncTime);
            
            if (newTransactions.isEmpty()) {
                System.out.println("📭 No new transactions found");
                return;
            }

            int processedCount = 0;
            LocalDateTime latestPunchTime = lastSyncTime;

            for (Map<String, Object> transaction : newTransactions) {
                try {
                    Integer transactionId = (Integer) transaction.get("id");
                    
                    // Skip if already processed
                    if (processedTransactionIds.contains(transactionId)) {
                        continue;
                    }

                    if (processTransaction(transaction)) {
                        processedCount++;
                        processedTransactionIds.add(transactionId);
                        
                        // Update latest punch time
                        Object punchTimeObj = transaction.get("punch_time");
                        LocalDateTime punchTime = parseDateTime(punchTimeObj);
                        if (punchTime != null && punchTime.isAfter(latestPunchTime)) {
                            latestPunchTime = punchTime;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error processing transaction " + transaction.get("id") + ": " + e.getMessage());
                }
            }

            // Update last sync time
            lastSyncTime = latestPunchTime;

            if (processedCount > 0) {
                System.out.println("✅ Processed " + processedCount + " new transactions");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in real-time sync: " + e.getMessage());
        }
    }

    /**
     * Process individual transaction
     */
    @Transactional
    private boolean processTransaction(Map<String, Object> transaction) {
        try {
            String empCode = (String) transaction.get("emp_code");
            String machineEmpIdStr = (String) transaction.get("emp_id");
            Object punchTimeObj = transaction.get("punch_time");
            String punchStateStr = (String) transaction.get("punch_state");
            String terminalSn = (String) transaction.get("terminal_sn");
            
            // Parse data
            LocalDateTime punchDateTime = parseDateTime(punchTimeObj);
            if (punchDateTime == null) {
                return false;
            }
            
            Integer punchState = safeParseInteger(punchStateStr);
            
            // Find employee
            Employee employee = findEmployee(empCode, machineEmpIdStr);
            if (employee == null) {
                System.out.println("⚠️ Employee not found for emp_code: " + empCode);
                return false;
            }

            // Create or update attendance
            boolean success = createOrUpdateAttendance(employee, punchDateTime, punchState, transaction);
            
            if (success) {
                System.out.println("✅ " + employee.getFullName() + " - " + 
                                 punchDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                                 " - " + (punchState != null && punchState == 0 ? "IN" : "OUT"));
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Error processing transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find employee using simple strategies
     */
    private Employee findEmployee(String empCode, String machineEmpIdStr) {
        try {
            // Strategy 1: Direct emp_code lookup
            if (empCode != null) {
                try {
                    Integer empId = Integer.parseInt(empCode);
                    Optional<Employee> empOpt = employeeRepo.findById(empId);
                    if (empOpt.isPresent()) {
                        return empOpt.get();
                    }
                } catch (NumberFormatException e) {
                    // Not a direct ID
                }
            }

            // Strategy 2: Parse machine emp_id (take last 6 digits)
            if (machineEmpIdStr != null && machineEmpIdStr.length() > 6) {
                try {
                    String shortId = machineEmpIdStr.substring(machineEmpIdStr.length() - 6);
                    Integer empId = Integer.parseInt(shortId);
                    Optional<Employee> empOpt = employeeRepo.findById(empId);
                    if (empOpt.isPresent()) {
                        return empOpt.get();
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("❌ Error finding employee: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create or update attendance record
     */
    @Transactional
    private boolean createOrUpdateAttendance(Employee employee, LocalDateTime punchDateTime, 
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
            attendance.setVerifyType("fingerprint");
            attendance.setPunchSource("BIOMETRIC");
            attendance.setRawData(transaction.toString());

            // Set punch time based on state
            if (punchState != null && punchState == 0) {
                // Punch IN
                attendance.setPunchInTime(punchTime);
                attendance.setPunchIn(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else {
                // Punch OUT
                attendance.setPunchOutTime(punchTime);
                attendance.setPunchOut(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

            // Calculate working hours if both times are set
            if (attendance.getPunchInTime() != null && attendance.getPunchOutTime() != null) {
                attendance.calculateDurations();
            }

            // Save attendance
            attendanceRepo.save(attendance);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error creating attendance: " + e.getMessage());
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
                
                return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely parse String to Integer
     */
    private Integer safeParseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get sync statistics
     */
    public Map<String, Object> getSyncStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("processedTransactionIds", processedTransactionIds.size());
        stats.put("lastSyncTime", lastSyncTime);
        stats.put("serviceName", "SimpleRealtimeBiometricService");
        return stats;
    }

    /**
     * Manual sync trigger
     */
    public Map<String, Object> triggerManualSync() {
        try {
            performRealtimeSync();
            return Map.of(
                "success", true,
                "message", "Manual sync completed",
                "processedCount", processedTransactionIds.size()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}
