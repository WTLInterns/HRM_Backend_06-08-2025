package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RealTimeAttendanceSyncService {

    @Autowired
    private EasyTimeProApiService easyTimeProApiService;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${easytime.sync.interval:30000}")
    private long syncInterval;

    private Set<String> processedTransactionIds = new HashSet<>();

    /**
     * Real-time sync of attendance data from EasyTimePro
     * Runs every 30 seconds by default
     */
    @Scheduled(fixedRateString = "${easytime.sync.interval:30000}")
    public void syncAttendanceData() {
        try {
            System.out.println("Starting real-time attendance sync...");

            List<SubadminTerminal> activeTerminals = terminalRepo.findByStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            for (SubadminTerminal terminal : activeTerminals) {
                syncTerminalAttendance(terminal);
            }

        } catch (Exception e) {
            System.err.println("Error in real-time attendance sync: " + e.getMessage());
        }
    }

    /**
     * Sync attendance data for a specific terminal
     */
    private void syncTerminalAttendance(SubadminTerminal terminal) {
        try {
            String apiUrl = terminal.getEasytimeApiUrl();
            String token = terminal.getApiToken();

            // Get token if not available
            if (token == null || token.isEmpty()) {
                token = easyTimeProApiService.authenticateAndGetToken(
                        apiUrl, "admin", "123456");
                if (token != null) {
                    terminal.setApiToken(token);
                    terminalRepo.save(terminal);
                }
            }

            if (token == null) {
                System.err.println("Failed to get token for terminal: " + terminal.getTerminalSerial());
                return;
            }

            // Get recent transactions (last 1 hour)
            String fromDate = LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String toDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            List<Map<String, Object>> transactions = easyTimeProApiService.getAttendanceTransactions(
                    apiUrl, token, fromDate, toDate);

            System.out.println(
                    "Found " + transactions.size() + " transactions for terminal: " + terminal.getTerminalSerial());

            for (Map<String, Object> transaction : transactions) {
                processTransaction(transaction, terminal);
            }

        } catch (Exception e) {
            System.err.println("Error syncing terminal " + terminal.getTerminalSerial() + ": " + e.getMessage());
        }
    }

    /**
     * Process individual attendance transaction
     */
    private void processTransaction(Map<String, Object> transaction, SubadminTerminal terminal) {
        try {
            // Extract transaction data
            String transactionId = String.valueOf(transaction.get("id"));
            Integer easytimeEmployeeId = (Integer) transaction.get("employee_id");
            String punchTimeStr = (String) transaction.get("punch_time");
            Integer punchState = (Integer) transaction.get("punch_state"); // 0=in, 1=out

            // Skip if already processed
            if (processedTransactionIds.contains(transactionId)) {
                return;
            }

            // Find employee mapping
            Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo
                    .findByEasytimeEmployeeIdAndTerminalSerial(easytimeEmployeeId, terminal.getTerminalSerial());

            if (mappingOpt.isEmpty()) {
                System.out.println("No mapping found for EasyTime employee " + easytimeEmployeeId +
                        " on terminal " + terminal.getTerminalSerial());
                return;
            }

            EmployeeDeviceMapping mapping = mappingOpt.get();
            Employee employee = employeeRepo.findById(mapping.getHrmEmployeeId()).orElse(null);

            if (employee == null) {
                System.err.println("Employee not found: " + mapping.getHrmEmployeeId());
                return;
            }

            // Parse punch time
            LocalDateTime punchDateTime = LocalDateTime.parse(punchTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();
            String dateStr = punchDate.toString();

            // Find or create attendance record
            Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(employee, dateStr);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Set punch time based on punch state
            String punchType;
            if (punchState == 0) { // Check in
                attendance.setPunchInTime(punchTime);
                punchType = "check_in";
            } else { // Check out
                attendance.setPunchOutTime(punchTime);
                punchType = "check_out";
            }

            // Calculate durations
            attendance.calculateDurations();

            // Save attendance
            Attendance savedAttendance = attendanceRepo.save(attendance);

            // Mark transaction as processed
            processedTransactionIds.add(transactionId);

            // Send real-time notification
            broadcastAttendanceUpdate(employee.getSubadmin().getId(), savedAttendance, punchType);

            System.out.println("Processed " + punchType + " for " + employee.getFullName() +
                    " at " + punchTime + " (Transaction: " + transactionId + ")");

        } catch (Exception e) {
            System.err.println("Error processing transaction: " + e.getMessage());
        }
    }

    /**
     * Broadcast real-time attendance update via WebSocket
     */
    private void broadcastAttendanceUpdate(Integer subadminId, Attendance attendance, String punchType) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "REAL_TIME_ATTENDANCE");
            update.put("employeeId", attendance.getEmployee().getEmpId());
            update.put("employeeName", attendance.getEmployee().getFullName());
            update.put("date", attendance.getDate());
            update.put("punchType", punchType);
            update.put("punchInTime",
                    attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : null);
            update.put("punchOutTime",
                    attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : null);
            update.put("source", "BIOMETRIC");
            update.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);

            System.out.println("Sent real-time update for " + attendance.getEmployee().getFullName());

        } catch (Exception e) {
            System.err.println("Error broadcasting attendance update: " + e.getMessage());
        }
    }

    /**
     * Manual sync for specific terminal
     */
    public void syncSpecificTerminal(String terminalSerial) {
        try {
            Optional<SubadminTerminal> terminalOpt = terminalRepo.findByTerminalSerial(terminalSerial);
            if (terminalOpt.isPresent()) {
                syncTerminalAttendance(terminalOpt.get());
                System.out.println("Manual sync completed for terminal: " + terminalSerial);
            } else {
                System.err.println("Terminal not found: " + terminalSerial);
            }
        } catch (Exception e) {
            System.err.println("Error in manual sync: " + e.getMessage());
        }
    }

    /**
     * Get sync status for all terminals
     */
    public Map<String, Object> getSyncStatus() {
        try {
            List<SubadminTerminal> terminals = terminalRepo.findAll();
            List<Map<String, Object>> terminalStatus = new ArrayList<>();

            for (SubadminTerminal terminal : terminals) {
                Map<String, Object> status = new HashMap<>();
                status.put("terminalSerial", terminal.getTerminalSerial());
                status.put("terminalName", terminal.getTerminalName());
                status.put("status", terminal.getStatus());
                status.put("lastSyncAt", terminal.getLastSyncAt());
                status.put("apiUrl", terminal.getEasytimeApiUrl());

                // Test connection
                boolean connected = false;
                if (terminal.getApiToken() != null) {
                    connected = easyTimeProApiService.testConnection(
                            terminal.getEasytimeApiUrl(), terminal.getApiToken());
                }
                status.put("connected", connected);

                terminalStatus.add(status);
            }

            Map<String, Object> syncStatus = new HashMap<>();
            syncStatus.put("terminals", terminalStatus);
            syncStatus.put("syncInterval", syncInterval);
            syncStatus.put("processedTransactions", processedTransactionIds.size());
            syncStatus.put("lastSyncTime", LocalDateTime.now().toString());

            return syncStatus;

        } catch (Exception e) {
            System.err.println("Error getting sync status: " + e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Clear processed transaction cache (for testing)
     */
    public void clearProcessedTransactions() {
        processedTransactionIds.clear();
        System.out.println("Cleared processed transaction cache");
    }

    /**
     * Force sync all terminals now
     */
    public void forceSyncAll() {
        try {
            System.out.println("Force syncing all terminals...");
            syncAttendanceData();
        } catch (Exception e) {
            System.err.println("Error in force sync: " + e.getMessage());
        }
    }
}
