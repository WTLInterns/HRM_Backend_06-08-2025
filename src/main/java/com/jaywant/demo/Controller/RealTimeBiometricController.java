package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Service.EasyTimeProApiService;
import com.jaywant.demo.Service.RealTimeAttendanceSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/realtime-biometric")
@CrossOrigin(origins = "*")
public class RealTimeBiometricController {

    @Autowired
    private EasyTimeProApiService easyTimeProApiService;

    @Autowired
    private RealTimeAttendanceSyncService syncService;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    /**
     * Register terminal with EasyTimePro integration
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

            String apiUrl = "http://" + terminalIp + ":" + terminalPort;

            // Test connection and get token
            String token = easyTimeProApiService.authenticateAndGetToken(apiUrl, "admin", "123456");
            if (token == null) {
                return ResponseEntity.badRequest().body("Failed to authenticate with EasyTimePro at " + apiUrl);
            }

            // Test API connection
            boolean connected = easyTimeProApiService.testConnection(apiUrl, token);
            if (!connected) {
                return ResponseEntity.badRequest().body("Failed to connect to EasyTimePro API at " + apiUrl);
            }

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
            terminal.setEasytimeApiUrl(apiUrl);
            terminal.setApiToken(token);
            terminal.setStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            SubadminTerminal savedTerminal = terminalRepo.save(terminal);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("terminal", savedTerminal);
            response.put("message", "Terminal registered successfully with EasyTimePro integration");
            response.put("realTimeSync", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering terminal: " + e.getMessage());
        }
    }

    /**
     * Register employee with full EasyTimePro integration
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

            // Create employee in EasyTimePro
            String empCode = generateEmpCode(employee, terminal);
            Map<String, Object> employeeData = easyTimeProApiService.createEmployeePayload(
                    empCode, employee.getFirstName(), employee.getLastName(),
                    nextMachineId, 1, 1);

            Map<String, Object> easytimeEmployee = easyTimeProApiService.registerEmployee(
                    terminal.getEasytimeApiUrl(), terminal.getApiToken(), employeeData);

            if (easytimeEmployee == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to register employee in EasyTimePro");
            }

            // Create mapping
            EmployeeDeviceMapping mapping = new EmployeeDeviceMapping();
            mapping.setHrmEmployeeId(empId);
            mapping.setSubadminId(employee.getSubadmin().getId());
            mapping.setEasytimeEmployeeId(nextMachineId);
            mapping.setTerminalSerial(terminalSerial);
            mapping.setEmpCode(empCode);
            mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.PENDING);

            EmployeeDeviceMapping savedMapping = mappingRepo.save(mapping);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employee", employee);
            response.put("terminal", terminal);
            response.put("machineEmployeeId", nextMachineId);
            response.put("empCode", empCode);
            response.put("easytimeEmployee", easytimeEmployee);
            response.put("nextStep", "Enroll fingerprint on F22 using Employee ID: " + nextMachineId);
            response.put("realTimeSync", true);
            response.put("instructions", java.util.List.of(
                    "1. Go to F22 machine",
                    "2. MENU → User Management → Add User",
                    "3. Enter Employee ID: " + nextMachineId,
                    "4. Enter Name: " + employee.getFullName(),
                    "5. Scan fingerprint 3-5 times",
                    "6. Attendance will automatically sync to database"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering employee: " + e.getMessage());
        }
    }

    /**
     * Get real-time sync status
     */
    @GetMapping("/sync/status")
    public ResponseEntity<?> getSyncStatus() {
        try {
            Map<String, Object> status = syncService.getSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting sync status: " + e.getMessage());
        }
    }

    /**
     * Force sync all terminals
     */
    @PostMapping("/sync/force")
    public ResponseEntity<?> forceSync() {
        try {
            syncService.forceSyncAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Force sync initiated for all terminals");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error forcing sync: " + e.getMessage());
        }
    }

    /**
     * Sync specific terminal
     */
    @PostMapping("/sync/terminal/{terminalSerial}")
    public ResponseEntity<?> syncTerminal(@PathVariable String terminalSerial) {
        try {
            syncService.syncSpecificTerminal(terminalSerial);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sync initiated for terminal: " + terminalSerial);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing terminal: " + e.getMessage());
        }
    }

    /**
     * Clear processed transaction cache
     */
    @PostMapping("/sync/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            syncService.clearProcessedTransactions();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Processed transaction cache cleared");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error clearing cache: " + e.getMessage());
        }
    }

    /**
     * Test EasyTimePro connection
     */
    @PostMapping("/test-connection")
    public ResponseEntity<?> testConnection(@RequestBody Map<String, Object> request) {
        try {
            String terminalIp = (String) request.get("terminalIp");
            String terminalPort = (String) request.get("terminalPort");
            String apiUrl = "http://" + terminalIp + ":" + terminalPort;

            // Test authentication
            String token = easyTimeProApiService.authenticateAndGetToken(apiUrl, "admin", "123456");
            if (token == null) {
                return ResponseEntity.badRequest().body("Authentication failed");
            }

            // Test API connection
            boolean connected = easyTimeProApiService.testConnection(apiUrl, token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", connected);
            response.put("apiUrl", apiUrl);
            response.put("authenticated", token != null);
            response.put("connected", connected);
            response.put("message", connected ? "Connection successful" : "Connection failed");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Connection test failed: " + e.getMessage());
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
}
