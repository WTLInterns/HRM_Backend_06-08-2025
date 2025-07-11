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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EasyTimeProIntegrationService {

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

    /**
     * Register employee on EasyTimePro terminal with ID verification
     */
    @Transactional
    public EmployeeDeviceMapping registerEmployeeOnTerminal(Integer hrmEmployeeId, String terminalSerial) {
        try {
            // Get employee and terminal info
            Employee employee = employeeRepo.findById(hrmEmployeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + hrmEmployeeId));

            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found: " + terminalSerial));

            // Check if already registered
            Optional<EmployeeDeviceMapping> existingMapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(hrmEmployeeId, terminalSerial);

            if (existingMapping.isPresent()) {
                throw new RuntimeException("Employee already registered on this terminal");
            }

            // Generate unique employee code and get next EasyTime ID
            String empCode = generateEmployeeCode(employee, terminal);
            Integer nextEasyTimeId = getNextEasyTimeEmployeeId(terminalSerial);

            // Create employee payload for EasyTimePro
            Map<String, Object> employeeData = easyTimeProApiService.createEmployeePayload(
                    empCode,
                    employee.getFirstName(),
                    employee.getLastName(),
                    nextEasyTimeId,
                    1, // Default department
                    1 // Default area
            );

            // Register in EasyTimePro
            Map<String, Object> response = easyTimeProApiService.registerEmployee(
                    terminal.getEasytimeApiUrl(),
                    terminal.getApiToken(),
                    employeeData);

            // Create mapping record
            EmployeeDeviceMapping mapping = new EmployeeDeviceMapping(
                    hrmEmployeeId,
                    employee.getSubadmin().getId(),
                    nextEasyTimeId,
                    terminalSerial,
                    empCode);
            mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.PENDING);

            EmployeeDeviceMapping savedMapping = mappingRepo.save(mapping);

            // Notify via WebSocket
            notifyEmployeeRegistration(employee, terminal, savedMapping);

            return savedMapping;

        } catch (Exception e) {
            throw new RuntimeException("Failed to register employee on terminal: " + e.getMessage(), e);
        }
    }

    /**
     * Process attendance transaction from EasyTimePro
     */
    @Transactional
    public void processAttendanceTransaction(Map<String, Object> transaction) {
        try {
            // Extract transaction data
            Integer employeeId = (Integer) transaction.get("employee_id");
            String terminalSn = (String) transaction.get("terminal_sn");
            String punchTimeStr = (String) transaction.get("punch_time");
            String punchType = (String) transaction.get("punch_type");

            // Find employee mapping
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByEasytimeEmployeeIdAndTerminalSerial(employeeId, terminalSn)
                    .orElse(null);

            if (mapping == null) {
                System.out.println("No mapping found for employee " + employeeId + " on terminal " + terminalSn);
                return;
            }

            // Get HRM employee
            Employee employee = employeeRepo.findById(mapping.getHrmEmployeeId()).orElse(null);
            if (employee == null) {
                System.out.println("HRM employee not found: " + mapping.getHrmEmployeeId());
                return;
            }

            // Parse punch time
            LocalDateTime punchDateTime = LocalDateTime.parse(punchTimeStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();

            // Find or create attendance record
            String dateStr = punchDate.toString();
            Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(employee, dateStr);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
            }

            // Update punch times based on type
            if ("check_in".equals(punchType) || "0".equals(punchType)) {
                attendance.setPunchInTime(punchTime);
            } else if ("check_out".equals(punchType) || "1".equals(punchType)) {
                attendance.setPunchOutTime(punchTime);
            }

            // Recalculate durations
            attendance.calculateDurations();

            // Save attendance
            Attendance savedAttendance = attendanceRepo.save(attendance);

            // Broadcast real-time update
            broadcastAttendanceUpdate(employee.getSubadmin().getId(), savedAttendance);

            System.out.println("Processed attendance: " + employee.getFullName() +
                    " - " + punchType + " at " + punchTime);

        } catch (Exception e) {
            System.err.println("Error processing attendance transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sync attendance data from EasyTimePro for a specific date range
     */
    public void syncAttendanceData(String terminalSerial, String fromDate, String toDate) {
        try {
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found: " + terminalSerial));

            // Get transactions from EasyTimePro
            List<Map<String, Object>> transactions = easyTimeProApiService.getAttendanceTransactions(
                    terminal.getEasytimeApiUrl(),
                    terminal.getApiToken(),
                    fromDate,
                    toDate);

            // Process each transaction
            for (Map<String, Object> transaction : transactions) {
                processAttendanceTransaction(transaction);
            }

            // Update last sync time
            terminal.setLastSyncAt(LocalDateTime.now());
            terminalRepo.save(terminal);

            System.out.println("Synced " + transactions.size() + " transactions from terminal " + terminalSerial);

        } catch (Exception e) {
            System.err.println("Error syncing attendance data: " + e.getMessage());
            throw new RuntimeException("Attendance sync failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get next available EasyTime employee ID for a terminal
     */
    private Integer getNextEasyTimeEmployeeId(String terminalSerial) {
        Integer maxId = mappingRepo.findNextEasytimeEmployeeId(terminalSerial);
        return maxId != null ? maxId : 1;
    }

    /**
     * Generate unique employee code
     */
    private String generateEmployeeCode(Employee employee, SubadminTerminal terminal) {
        return String.format("SA%d_EMP%d_%s",
                employee.getSubadmin().getId(),
                employee.getEmpId(),
                terminal.getTerminalSerial().substring(Math.max(0, terminal.getTerminalSerial().length() - 4)));
    }

    /**
     * Notify frontend about employee registration
     */
    private void notifyEmployeeRegistration(Employee employee, SubadminTerminal terminal,
            EmployeeDeviceMapping mapping) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "EMPLOYEE_REGISTERED");
        notification.put("employeeId", employee.getEmpId());
        notification.put("employeeName", employee.getFullName());
        notification.put("terminalSerial", terminal.getTerminalSerial());
        notification.put("terminalName", terminal.getTerminalName());
        notification.put("empCode", mapping.getEmpCode());
        notification.put("easytimeEmployeeId", mapping.getEasytimeEmployeeId());

        messagingTemplate.convertAndSend("/topic/employee/" + employee.getSubadmin().getId(), notification);
    }

    /**
     * Broadcast attendance update via WebSocket
     */
    private void broadcastAttendanceUpdate(Integer subadminId, Attendance attendance) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "BIOMETRIC_ATTENDANCE");
        update.put("employeeId", attendance.getEmployee().getEmpId());
        update.put("employeeName", attendance.getEmployee().getFullName());
        update.put("date", attendance.getDate());
        update.put("punchInTime", attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : null);
        update.put("punchOutTime",
                attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : null);
        update.put("status", attendance.getStatus());
        update.put("source", "BIOMETRIC");

        messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);
    }

    /**
     * Get employee mappings for a subadmin
     */
    public List<EmployeeDeviceMapping> getEmployeeMappings(Integer subadminId) {
        return mappingRepo.findBySubadminIdWithEmployeeDetails(subadminId);
    }

    /**
     * Verify and update actual employee ID assigned by machine
     */
    @Transactional
    public EmployeeDeviceMapping verifyAndUpdateMachineEmployeeId(Integer hrmEmployeeId, String terminalSerial) {
        try {
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(hrmEmployeeId, terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Employee mapping not found"));

            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found"));

            // Get all employees from EasyTimePro to find actual assigned ID
            List<Map<String, Object>> employees = easyTimeProApiService.getEmployees(
                    terminal.getEasytimeApiUrl(),
                    terminal.getApiToken());

            // Find employee by emp_code
            String empCode = mapping.getEmpCode();
            for (Map<String, Object> emp : employees) {
                if (empCode.equals(emp.get("emp_code"))) {
                    Integer actualEmployeeId = (Integer) emp.get("employee_id");

                    if (!actualEmployeeId.equals(mapping.getEasytimeEmployeeId())) {
                        System.out.println("⚠️ Employee ID mismatch detected!");
                        System.out.println("Expected: " + mapping.getEasytimeEmployeeId());
                        System.out.println("Actual: " + actualEmployeeId);

                        // Update mapping with actual ID
                        mapping.setEasytimeEmployeeId(actualEmployeeId);
                        mapping = mappingRepo.save(mapping);

                        System.out.println("✅ Updated mapping with actual employee ID: " + actualEmployeeId);
                    }
                    break;
                }
            }

            return mapping;

        } catch (Exception e) {
            System.err.println("Error verifying employee ID: " + e.getMessage());
            throw new RuntimeException("Failed to verify employee ID: " + e.getMessage(), e);
        }
    }

    /**
     * Get all employees from EasyTimePro for verification
     */
    public List<Map<String, Object>> getEmployeesFromMachine(String terminalSerial) {
        try {
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found"));

            return easyTimeProApiService.getEmployees(
                    terminal.getEasytimeApiUrl(),
                    terminal.getApiToken());

        } catch (Exception e) {
            throw new RuntimeException("Failed to get employees from machine: " + e.getMessage(), e);
        }
    }

    /**
     * Remove employee from terminal
     */
    @Transactional
    public boolean removeEmployeeFromTerminal(Integer hrmEmployeeId, String terminalSerial) {
        try {
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(hrmEmployeeId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                return false;
            }

            // Remove from EasyTimePro
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial).orElse(null);
            if (terminal != null) {
                easyTimeProApiService.deleteEmployee(
                        terminal.getEasytimeApiUrl(),
                        terminal.getApiToken(),
                        mapping.getEasytimeEmployeeId());
            }

            // Remove mapping
            mappingRepo.delete(mapping);
            return true;

        } catch (Exception e) {
            System.err.println("Error removing employee from terminal: " + e.getMessage());
            return false;
        }
    }
}
