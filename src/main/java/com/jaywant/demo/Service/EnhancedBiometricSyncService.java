package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

@Service
public class EnhancedBiometricSyncService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Cache for processed transactions to avoid duplicates
    private final Set<String> processedTransactions = ConcurrentHashMap.newKeySet();

    // Cache for device-subadmin mapping
    private final Map<String, Integer> deviceSubadminCache = new ConcurrentHashMap<>();

    // Last sync timestamp per device
    private final Map<String, LocalDateTime> lastSyncPerDevice = new ConcurrentHashMap<>();

    /**
     * Real-time sync every 10 seconds with comprehensive logging
     * DISABLED: Replaced by SimpleRealtimeBiometricService
     */
    // @Scheduled(fixedRate = 10000) // 10 seconds - DISABLED
    public void performRealTimeBiometricSync() {
        try {
            System.out.println("🔄 [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + "] Starting enhanced biometric sync...");

            // Get all active terminals/devices
            List<SubadminTerminal> activeTerminals = getActiveTerminals();

            if (activeTerminals.isEmpty()) {
                System.out.println("⚠️ No active terminals found for sync");
                return;
            }

            int totalProcessed = 0;
            int totalErrors = 0;

            for (SubadminTerminal terminal : activeTerminals) {
                try {
                    int processed = syncDeviceTransactions(terminal);
                    totalProcessed += processed;

                    if (processed > 0) {
                        System.out.println("✅ Device [" + terminal.getTerminalSerial() + "] - Subadmin [" +
                                terminal.getSubadminId() + "] - Processed: " + processed + " transactions");
                    }

                } catch (Exception e) {
                    totalErrors++;
                    System.err.println(
                            "❌ Error syncing device [" + terminal.getTerminalSerial() + "]: " + e.getMessage());
                }
            }

            if (totalProcessed > 0 || totalErrors > 0) {
                System.out.println("📊 Sync Summary - Processed: " + totalProcessed + ", Errors: " + totalErrors +
                        ", Active Devices: " + activeTerminals.size());
            }

        } catch (Exception e) {
            System.err.println("❌ Critical error in biometric sync: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all active terminals with their subadmin mapping
     */
    private List<SubadminTerminal> getActiveTerminals() {
        try {
            // Use findAll and filter for active status (handle both String and Enum)
            return terminalRepo.findAll().stream()
                    .filter(terminal -> {
                        try {
                            // Check if status is String "ACTIVE" or Enum ACTIVE
                            Object statusObj = terminal.getStatus();
                            if (statusObj != null && statusObj.toString().equals("ACTIVE")) {
                                return true;
                            }

                            return false;
                        } catch (Exception e) {
                            System.err.println("⚠️ Error checking terminal status: " + e.getMessage());
                            return false;
                        }
                    })
                    .toList();
        } catch (Exception e) {
            System.err.println("❌ Error fetching active terminals: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Sync transactions for a specific device
     */
    @Transactional
    private int syncDeviceTransactions(SubadminTerminal terminal) {
        try {
            String terminalSerial = terminal.getTerminalSerial();
            Integer subadminId = terminal.getSubadminId();

            // Cache device-subadmin mapping
            deviceSubadminCache.put(terminalSerial, subadminId);

            // Get last sync time for this device
            LocalDateTime lastSync = lastSyncPerDevice.getOrDefault(terminalSerial,
                    LocalDateTime.now().minusHours(24));

            // Query iclock_transaction table for this specific device
            String sql = """
                    SELECT
                        id, emp_code, punch_time, punch_state, verify_type,
                        terminal_sn, emp_id, terminal_id, company_id,
                        upload_time, source, work_code
                    FROM easywdms.iclock_transaction
                    WHERE terminal_sn = ?
                    AND punch_time > ?
                    AND punch_time <= NOW()
                    ORDER BY punch_time ASC
                    LIMIT 100
                    """;

            List<Map<String, Object>> transactions = jdbcTemplate.queryForList(sql,
                    terminalSerial,
                    lastSync);

            int processedCount = 0;
            LocalDateTime latestPunchTime = lastSync;

            for (Map<String, Object> transaction : transactions) {
                try {
                    if (processTransaction(transaction, terminal)) {
                        processedCount++;

                        // Update latest punch time
                        Object punchTimeObj = transaction.get("punch_time");
                        if (punchTimeObj != null) {
                            LocalDateTime punchTime = parseDateTime(punchTimeObj);
                            if (punchTime != null && punchTime.isAfter(latestPunchTime)) {
                                latestPunchTime = punchTime;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                            "❌ Error processing transaction ID " + transaction.get("id") + ": " + e.getMessage());
                }
            }

            // Update last sync time for this device
            lastSyncPerDevice.put(terminalSerial, latestPunchTime);

            return processedCount;

        } catch (Exception e) {
            System.err.println(
                    "❌ Error in syncDeviceTransactions for " + terminal.getTerminalSerial() + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Process individual transaction with comprehensive employee mapping
     */
    @Transactional
    private boolean processTransaction(Map<String, Object> transaction, SubadminTerminal terminal) {
        try {
            // Create unique transaction identifier
            String transactionId = createTransactionId(transaction);

            if (processedTransactions.contains(transactionId)) {
                return false; // Already processed
            }

            // Extract transaction data with safe type conversion
            String empCode = (String) transaction.get("emp_code");
            Integer machineEmpId = safeParseInteger(transaction.get("emp_id"));
            String terminalSerial = terminal.getTerminalSerial();
            Integer subadminId = terminal.getSubadminId();

            Object punchTimeObj = transaction.get("punch_time");
            Integer punchState = safeParseInteger(transaction.get("punch_state"));
            Integer verifyType = safeParseInteger(transaction.get("verify_type"));

            // Validate required data
            if (punchTimeObj == null) {
                System.out.println("⚠️ Transaction " + transaction.get("id") + " - No punch time");
                return false;
            }

            LocalDateTime punchDateTime = parseDateTime(punchTimeObj);
            if (punchDateTime == null) {
                System.out.println("⚠️ Transaction " + transaction.get("id") + " - Invalid punch time format");
                return false;
            }

            // Find HRM employee using multiple strategies
            Employee hrmEmployee = findHrmEmployee(empCode, machineEmpId, terminalSerial, subadminId);

            if (hrmEmployee == null) {
                System.out.println("⚠️ Transaction " + transaction.get("id") +
                        " - No HRM employee found for emp_code: " + empCode +
                        ", machine_emp_id: " + machineEmpId +
                        ", device: " + terminalSerial +
                        ", subadmin: " + subadminId);
                return false;
            }

            // Verify employee belongs to correct subadmin
            if (hrmEmployee.getSubadmin() == null ||
                    !Objects.equals(hrmEmployee.getSubadmin().getId(), subadminId)) {
                System.out.println("⚠️ Transaction " + transaction.get("id") +
                        " - Employee " + hrmEmployee.getFullName() +
                        " does not belong to subadmin " + subadminId);
                return false;
            }

            // Process attendance record
            boolean success = createOrUpdateAttendance(hrmEmployee, punchDateTime, punchState, verifyType, transaction);

            if (success) {
                processedTransactions.add(transactionId);

                System.out.println("✅ Transaction " + transaction.get("id") +
                        " - Employee: " + hrmEmployee.getFullName() +
                        " (" + hrmEmployee.getEmpId() + ")" +
                        " - Time: " + punchDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                        " - Type: " + (punchState != null && punchState == 0 ? "IN" : "OUT") +
                        " - Device: " + terminalSerial);

                // Send real-time notification
                sendRealtimeNotification(hrmEmployee, punchDateTime, punchState, terminalSerial);
            }

            return success;

        } catch (Exception e) {
            System.err.println("❌ Error processing transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find HRM employee using multiple mapping strategies
     */
    private Employee findHrmEmployee(String empCode, Integer machineEmpId, String terminalSerial, Integer subadminId) {
        try {
            System.out.println("🔍 Looking for employee - emp_code: " + empCode +
                    ", machine_emp_id: " + machineEmpId +
                    ", device: " + terminalSerial +
                    ", subadmin: " + subadminId);

            // Strategy 1: Find by device mapping (most reliable)
            if (empCode != null) {
                Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo
                        .findByEmpCodeAndTerminalSerialAndSubadminId(empCode, terminalSerial, subadminId);

                if (mappingOpt.isPresent()) {
                    Employee employee = employeeRepo.findById(mappingOpt.get().getHrmEmployeeId()).orElse(null);
                    if (employee != null) {
                        System.out.println("✅ Found via device mapping: " + employee.getFullName());
                        return employee;
                    }
                }
            }

            // Strategy 2: Find by machine emp_id within subadmin context
            if (machineEmpId != null) {
                Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo
                        .findByEasytimeEmployeeIdAndTerminalSerialAndSubadminId(machineEmpId, terminalSerial,
                                subadminId);

                if (mappingOpt.isPresent()) {
                    Employee employee = employeeRepo.findById(mappingOpt.get().getHrmEmployeeId()).orElse(null);
                    if (employee != null) {
                        System.out.println("✅ Found via machine emp_id mapping: " + employee.getFullName());
                        return employee;
                    }
                }
            }

            // Strategy 3: Parse emp_code format (SA{subadmin}_EMP{empId}_{terminal})
            if (empCode != null && empCode.contains("_EMP")) {
                Integer parsedEmpId = parseEmployeeIdFromCode(empCode);
                if (parsedEmpId != null) {
                    Employee employee = employeeRepo.findByEmpIdAndSubadminId(parsedEmpId, subadminId);
                    if (employee != null) {
                        System.out.println("✅ Found via emp_code parsing: " + employee.getFullName());
                        return employee;
                    }
                }
            }

            // Strategy 4: Direct machine emp_id lookup within subadmin
            if (machineEmpId != null) {
                Employee employee = employeeRepo.findByEmpIdAndSubadminId(machineEmpId, subadminId);
                if (employee != null) {
                    System.out.println("✅ Found via direct machine emp_id: " + employee.getFullName());
                    return employee;
                }
            }

            System.out.println("❌ No employee found with any strategy");
            return null;

        } catch (Exception e) {
            System.err.println("❌ Error in findHrmEmployee: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse employee ID from emp_code format
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
     * Create or update attendance record
     */
    @Transactional
    private boolean createOrUpdateAttendance(Employee employee, LocalDateTime punchDateTime,
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

            // Determine punch type and set appropriate time
            String punchType = determinePunchType(attendance, punchTime, punchState);

            if ("punch_in".equals(punchType)) {
                attendance.setPunchInTime(punchTime);
                attendance.setPunchIn(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } else if ("punch_out".equals(punchType)) {
                attendance.setPunchOutTime(punchTime);
                attendance.setPunchOut(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

            // Calculate working hours and durations
            attendance.calculateDurations();

            // Save attendance
            attendanceRepo.save(attendance);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Error creating/updating attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Determine punch type based on state and existing data
     */
    private String determinePunchType(Attendance attendance, LocalTime punchTime, Integer punchState) {
        // Use punch_state if available (0=in, 1=out)
        if (punchState != null) {
            return (punchState == 0) ? "punch_in" : "punch_out";
        }

        // Smart logic based on existing punches
        if (attendance.getPunchInTime() == null) {
            return "punch_in";
        } else if (attendance.getPunchOutTime() == null) {
            return "punch_out";
        } else {
            // Both exist - update based on time comparison
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
     * Create unique transaction identifier
     */
    private String createTransactionId(Map<String, Object> transaction) {
        return "TXN_" + transaction.get("id") + "_" +
                transaction.get("terminal_sn") + "_" +
                transaction.get("emp_code") + "_" +
                transaction.get("punch_time");
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
                return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing datetime: " + dateTimeObj + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Send real-time WebSocket notification
     */
    private void sendRealtimeNotification(Employee employee, LocalDateTime punchDateTime,
            Integer punchState, String terminalSerial) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "BIOMETRIC_PUNCH");
            notification.put("employeeId", employee.getEmpId());
            notification.put("employeeName", employee.getFullName());
            notification.put("subadminId", employee.getSubadmin().getId());
            notification.put("punchTime", punchDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            notification.put("punchType", (punchState != null && punchState == 0) ? "CHECK_IN" : "CHECK_OUT");
            notification.put("deviceSerial", terminalSerial);
            notification.put("timestamp", LocalDateTime.now());

            // Send to subadmin-specific topic
            messagingTemplate.convertAndSend("/topic/attendance/" + employee.getSubadmin().getId(), notification);

            // Send to general attendance topic
            messagingTemplate.convertAndSend("/topic/attendance", notification);

        } catch (Exception e) {
            System.err.println("❌ Error sending real-time notification: " + e.getMessage());
        }
    }

    /**
     * Manual sync trigger for testing
     */
    public Map<String, Object> triggerManualSync() {
        try {
            System.out.println("🔄 Manual sync triggered at " + LocalDateTime.now());
            performRealTimeBiometricSync();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Manual sync completed");
            result.put("timestamp", LocalDateTime.now());
            result.put("processedTransactionsCount", processedTransactions.size());
            result.put("activeDevicesCount", deviceSubadminCache.size());

            return result;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Manual sync failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Get sync statistics
     */
    public Map<String, Object> getSyncStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("processedTransactionsCount", processedTransactions.size());
        stats.put("activeDevicesCount", deviceSubadminCache.size());
        stats.put("deviceSubadminMapping", new HashMap<>(deviceSubadminCache));
        stats.put("lastSyncTimes", new HashMap<>(lastSyncPerDevice));
        return stats;
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
