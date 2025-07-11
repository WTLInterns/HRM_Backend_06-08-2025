package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/simple-biometric")
@CrossOrigin(origins = "*")
public class SimpleBiometricController {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.jaywant.demo.Service.F22DataSyncService f22DataSyncService;

    /**
     * Register biometric terminal (simplified)
     */
    @PostMapping("/terminals/register")
    public ResponseEntity<?> registerTerminal(@RequestBody Map<String, Object> request) {
        try {
            Integer subadminId = (Integer) request.get("subadminId");
            String terminalSerial = (String) request.get("terminalSerial");
            String terminalName = (String) request.get("terminalName");
            String location = (String) request.get("location");
            String terminalIp = (String) request.get("terminalIp");
            String terminalPort = (String) request.get("terminalPort");

            // Check if terminal already exists
            Optional<SubadminTerminal> existing = terminalRepo.findByTerminalSerial(terminalSerial);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body("Terminal already registered");
            }

            // Create terminal record
            SubadminTerminal terminal = new SubadminTerminal();
            terminal.setSubadminId(subadminId);
            terminal.setTerminalSerial(terminalSerial);
            terminal.setTerminalName(terminalName);
            terminal.setLocation(location);
            terminal.setEasytimeApiUrl("http://" + terminalIp + ":" + terminalPort);
            terminal.setStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            SubadminTerminal savedTerminal = terminalRepo.save(terminal);

            // Create safe terminal response without Hibernate proxies
            Map<String, Object> terminalData = new HashMap<>();
            terminalData.put("id", savedTerminal.getId());
            terminalData.put("subadminId", savedTerminal.getSubadminId());
            terminalData.put("terminalSerial", savedTerminal.getTerminalSerial());
            terminalData.put("terminalName", savedTerminal.getTerminalName());
            terminalData.put("location", savedTerminal.getLocation());
            terminalData.put("easytimeApiUrl", savedTerminal.getEasytimeApiUrl());
            terminalData.put("status", savedTerminal.getStatus().toString());
            terminalData.put("createdAt", savedTerminal.getCreatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("terminal", terminalData);
            response.put("message", "Terminal registered successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering terminal: " + e.getMessage());
        }
    }

    /**
     * Register employee on biometric terminal (simplified)
     */
    @PostMapping("/employee/register")
    public ResponseEntity<?> registerEmployee(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");

            // Validate employee exists
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            // Validate terminal exists
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial).orElse(null);
            if (terminal == null) {
                return ResponseEntity.badRequest().body("Terminal not found");
            }

            // Check if already registered
            Optional<EmployeeDeviceMapping> existing = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial);

            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body("Employee already registered on this terminal");
            }

            // Get next machine employee ID
            Integer nextMachineId = getNextMachineEmployeeId(terminalSerial);

            // Create mapping
            EmployeeDeviceMapping mapping = new EmployeeDeviceMapping();
            mapping.setHrmEmployeeId(empId);
            mapping.setSubadminId(employee.getSubadmin().getId());
            mapping.setEasytimeEmployeeId(nextMachineId);
            mapping.setTerminalSerial(terminalSerial);
            mapping.setEmpCode(generateEmpCode(employee, terminal));
            mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.PENDING);

            EmployeeDeviceMapping savedMapping = mappingRepo.save(mapping);

            // Create safe response objects without Hibernate proxies
            Map<String, Object> employeeData = new HashMap<>();
            employeeData.put("empId", employee.getEmpId());
            employeeData.put("fullName", employee.getFullName());
            employeeData.put("firstName", employee.getFirstName());
            employeeData.put("lastName", employee.getLastName());
            employeeData.put("email", employee.getEmail());

            Map<String, Object> terminalData = new HashMap<>();
            terminalData.put("id", terminal.getId());
            terminalData.put("terminalSerial", terminal.getTerminalSerial());
            terminalData.put("terminalName", terminal.getTerminalName());
            terminalData.put("location", terminal.getLocation());
            terminalData.put("status", terminal.getStatus().toString());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employee", employeeData);
            response.put("terminal", terminalData);
            response.put("machineEmployeeId", nextMachineId);
            response.put("empCode", savedMapping.getEmpCode());
            response.put("nextStep", "Enroll fingerprint on F22 using Employee ID: " + nextMachineId);
            response.put("instructions", List.of(
                    "1. Go to F22 machine",
                    "2. MENU → User Management → Add User",
                    "3. Enter Employee ID: " + nextMachineId,
                    "4. Enter Name: " + employee.getFullName(),
                    "5. Scan fingerprint 3-5 times",
                    "6. Confirm enrollment successful"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering employee: " + e.getMessage());
        }
    }

    /**
     * Add biometric attendance (manual entry simulating machine data)
     */
    @PostMapping("/attendance/add")
    public ResponseEntity<?> addBiometricAttendance(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");
            String punchType = (String) request.get("punchType"); // "check_in" or "check_out"
            String punchTime = (String) request.get("punchTime"); // "09:30"
            String date = (String) request.get("date"); // "2025-01-08"

            // Find employee mapping
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                return ResponseEntity.badRequest().body("Employee not registered on this terminal");
            }

            // Get employee
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            // Find or create attendance record
            Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(employee, date);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(date);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Parse and set punch time
            LocalTime time = LocalTime.parse(punchTime, DateTimeFormatter.ofPattern("HH:mm"));

            if ("check_in".equals(punchType)) {
                attendance.setPunchInTime(time);
            } else if ("check_out".equals(punchType)) {
                attendance.setPunchOutTime(time);
            }

            // Calculate durations
            attendance.calculateDurations();

            // Save attendance
            Attendance savedAttendance = attendanceRepo.save(attendance);

            // Send real-time update
            broadcastAttendanceUpdate(employee.getSubadmin().getId(), savedAttendance, punchType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("attendance", savedAttendance);
            response.put("message", "Biometric attendance recorded successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error recording attendance: " + e.getMessage());
        }
    }

    /**
     * Confirm fingerprint enrollment
     */
    @PostMapping("/confirm-enrollment")
    public ResponseEntity<?> confirmEnrollment(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");

            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                return ResponseEntity.badRequest().body("Employee not registered on this terminal");
            }

            mapping.setFingerprintEnrolled(true);
            mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.COMPLETED);
            mappingRepo.save(mapping);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fingerprint enrollment confirmed");
            response.put("machineEmployeeId", mapping.getEasytimeEmployeeId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming enrollment: " + e.getMessage());
        }
    }

    /**
     * Get employee biometric status
     */
    @GetMapping("/employees/{subadminId}/status")
    public ResponseEntity<?> getEmployeeStatus(@PathVariable Integer subadminId) {
        try {
            List<Employee> employees = employeeRepo.findBySubadminId(subadminId);
            List<SubadminTerminal> terminals = terminalRepo.findBySubadminId(subadminId);
            List<EmployeeDeviceMapping> mappings = mappingRepo.findBySubadminId(subadminId);

            List<Map<String, Object>> employeeStatus = employees.stream().map(emp -> {
                Map<String, Object> status = new HashMap<>();
                status.put("empId", emp.getEmpId());
                status.put("fullName", emp.getFullName());

                List<EmployeeDeviceMapping> empMappings = mappings.stream()
                        .filter(m -> m.getHrmEmployeeId().equals(emp.getEmpId()))
                        .toList();

                status.put("registeredTerminals", empMappings.size());
                status.put("fingerprintEnrolled", empMappings.stream()
                        .anyMatch(EmployeeDeviceMapping::getFingerprintEnrolled));
                status.put("mappings", empMappings);

                return status;
            }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("employees", employeeStatus);
            response.put("terminals", terminals);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching status: " + e.getMessage());
        }
    }

    // Helper methods
    private Integer getNextMachineEmployeeId(String terminalSerial) {
        Integer maxId = mappingRepo.findNextEasytimeEmployeeId(terminalSerial);
        return maxId != null ? maxId : 1;
    }

    private String generateEmpCode(Employee employee, SubadminTerminal terminal) {
        return String.format("SA%d_EMP%d_%s",
                employee.getSubadmin().getId(),
                employee.getEmpId(),
                terminal.getTerminalSerial().substring(Math.max(0, terminal.getTerminalSerial().length() - 4)));
    }

    private void broadcastAttendanceUpdate(Integer subadminId, Attendance attendance, String punchType) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "BIOMETRIC_ATTENDANCE");
        update.put("employeeId", attendance.getEmployee().getEmpId());
        update.put("employeeName", attendance.getEmployee().getFullName());
        update.put("date", attendance.getDate());
        update.put("punchType", punchType);
        update.put("punchInTime", attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : null);
        update.put("punchOutTime",
                attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : null);
        update.put("source", "BIOMETRIC");

        messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);
    }

    /**
     * Manual sync punch data from F22 machine
     */
    @PostMapping("/sync-punch")
    public ResponseEntity<?> syncPunchData(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");
            String punchType = (String) request.get("punchType");
            String punchTime = (String) request.get("punchTime");

            f22DataSyncService.syncPunchData(empId, terminalSerial, punchType, punchTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Punch data synced successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing punch data: " + e.getMessage());
        }
    }
}
