package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Service.EasyTimeProIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/biometric")
@CrossOrigin(origins = "*")
public class BiometricRegistrationController {

    @Autowired
    private EasyTimeProIntegrationService integrationService;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    /**
     * Register existing employee on punch machine
     */
    @PostMapping("/register-employee")
    public ResponseEntity<?> registerEmployeeOnMachine(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");

            // Validate employee exists
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found with ID: " + empId);
            }

            // Validate terminal exists
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial).orElse(null);
            if (terminal == null) {
                return ResponseEntity.badRequest().body("Terminal not found: " + terminalSerial);
            }

            // Check if already registered
            Optional<EmployeeDeviceMapping> existing = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial);

            if (existing.isPresent()) {
                return ResponseEntity.badRequest()
                        .body("Employee already registered on this terminal. Machine Employee ID: " +
                                existing.get().getEasytimeEmployeeId());
            }

            // Register employee on machine
            EmployeeDeviceMapping mapping = integrationService.registerEmployeeOnTerminal(empId, terminalSerial);

            // Prepare response with next steps
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employee", employee);
            response.put("terminal", terminal);
            response.put("machineEmployeeId", mapping.getEasytimeEmployeeId());
            response.put("empCode", mapping.getEmpCode());
            response.put("nextStep",
                    "Go to punch machine and enroll fingerprint using Employee ID: " + mapping.getEasytimeEmployeeId());
            response.put("instructions", List.of(
                    "1. Go to the punch machine: " + terminal.getTerminalName(),
                    "2. Access Admin Menu → User Management → Add User",
                    "3. Enter Employee ID: " + mapping.getEasytimeEmployeeId(),
                    "4. Enter Name: " + employee.getFullName(),
                    "5. Select Fingerprint Enrollment",
                    "6. Scan finger 3-5 times until successful",
                    "7. Test punch in/out to confirm working"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering employee: " + e.getMessage());
        }
    }

    /**
     * Get all employees for a subadmin with their biometric registration status
     */
    @GetMapping("/employees/{subadminId}/status")
    public ResponseEntity<?> getEmployeeBiometricStatus(@PathVariable Integer subadminId) {
        try {
            // Get all employees for subadmin
            List<Employee> employees = employeeRepo.findBySubadminId(subadminId);

            // Get all terminals for subadmin
            List<SubadminTerminal> terminals = terminalRepo.findBySubadminId(subadminId);

            // Get all mappings for subadmin
            List<EmployeeDeviceMapping> mappings = mappingRepo.findBySubadminId(subadminId);

            // Build response with status for each employee
            List<Map<String, Object>> employeeStatus = employees.stream().map(emp -> {
                Map<String, Object> status = new HashMap<>();
                status.put("empId", emp.getEmpId());
                status.put("fullName", emp.getFullName());
                status.put("email", emp.getEmail());

                // Check biometric registration status
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
            response.put("totalEmployees", employees.size());
            response.put("totalTerminals", terminals.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching employee status: " + e.getMessage());
        }
    }

    /**
     * Mark fingerprint enrollment as completed
     */
    @PostMapping("/confirm-enrollment")
    public ResponseEntity<?> confirmFingerprintEnrollment(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");

            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                return ResponseEntity.badRequest()
                        .body("Employee not registered on this terminal");
            }

            // Update enrollment status
            mapping.setFingerprintEnrolled(true);
            mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.COMPLETED);
            mappingRepo.save(mapping);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Fingerprint enrollment confirmed");
            response.put("employee", mapping.getEmployee().getFullName());
            response.put("machineEmployeeId", mapping.getEasytimeEmployeeId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming enrollment: " + e.getMessage());
        }
    }

    /**
     * Test punch data flow for an employee
     */
    @PostMapping("/test-punch")
    public ResponseEntity<?> testPunchDataFlow(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");
            String punchType = (String) request.get("punchType"); // "check_in" or "check_out"

            // Find mapping
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                return ResponseEntity.badRequest()
                        .body("Employee not registered on this terminal");
            }

            // Simulate punch transaction
            Map<String, Object> testTransaction = new HashMap<>();
            testTransaction.put("employee_id", mapping.getEasytimeEmployeeId());
            testTransaction.put("terminal_sn", terminalSerial);
            testTransaction.put("punch_time", java.time.LocalDateTime.now().toString().replace("T", " "));
            testTransaction.put("punch_type", punchType);
            testTransaction.put("verify_type", "fingerprint");

            // Process the test transaction
            integrationService.processAttendanceTransaction(testTransaction);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test punch processed successfully");
            response.put("testTransaction", testTransaction);
            response.put("note", "Check attendance records to confirm data was saved");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error testing punch: " + e.getMessage());
        }
    }

    /**
     * Verify actual employee ID assigned by machine
     */
    @PostMapping("/verify-employee-id")
    public ResponseEntity<?> verifyEmployeeId(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");

            EmployeeDeviceMapping updatedMapping = integrationService
                    .verifyAndUpdateMachineEmployeeId(empId, terminalSerial);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hrmEmployeeId", updatedMapping.getHrmEmployeeId());
            response.put("machineEmployeeId", updatedMapping.getEasytimeEmployeeId());
            response.put("empCode", updatedMapping.getEmpCode());
            response.put("message", "Employee ID verified and updated if necessary");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error verifying employee ID: " + e.getMessage());
        }
    }

    /**
     * Get all employees from machine for debugging
     */
    @GetMapping("/machine-employees/{terminalSerial}")
    public ResponseEntity<?> getMachineEmployees(@PathVariable String terminalSerial) {
        try {
            List<Map<String, Object>> employees = integrationService.getEmployeesFromMachine(terminalSerial);

            Map<String, Object> response = new HashMap<>();
            response.put("terminalSerial", terminalSerial);
            response.put("employees", employees);
            response.put("count", employees.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching machine employees: " + e.getMessage());
        }
    }

    /**
     * Get registration instructions for a specific employee and terminal
     */
    @GetMapping("/instructions/{empId}/{terminalSerial}")
    public ResponseEntity<?> getRegistrationInstructions(
            @PathVariable Integer empId,
            @PathVariable String terminalSerial) {
        try {
            Employee employee = employeeRepo.findById(empId).orElse(null);
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial).orElse(null);

            if (employee == null || terminal == null) {
                return ResponseEntity.badRequest().body("Employee or terminal not found");
            }

            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            Map<String, Object> instructions = new HashMap<>();
            instructions.put("employee", employee.getFullName());
            instructions.put("terminal", terminal.getTerminalName());
            instructions.put("location", terminal.getLocation());

            if (mapping != null) {
                instructions.put("machineEmployeeId", mapping.getEasytimeEmployeeId());
                instructions.put("status", mapping.getEnrollmentStatus());
                instructions.put("fingerprintEnrolled", mapping.getFingerprintEnrolled());

                instructions.put("steps", List.of(
                        "1. Go to punch machine: " + terminal.getTerminalName() + " (" + terminal.getLocation() + ")",
                        "2. Access Admin Menu (may require admin password)",
                        "3. Navigate to: User Management → Add User",
                        "4. Enter Employee ID: " + mapping.getEasytimeEmployeeId(),
                        "5. Enter Employee Name: " + employee.getFullName(),
                        "6. Select: Fingerprint Enrollment",
                        "7. Place finger on scanner when prompted",
                        "8. Scan the same finger 3-5 times for accuracy",
                        "9. Confirm enrollment is successful",
                        "10. Test by doing a practice punch in/out"));
            } else {
                instructions.put("error", "Employee not registered on this terminal. Please register first.");
            }

            return ResponseEntity.ok(instructions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting instructions: " + e.getMessage());
        }
    }

    /**
     * Store biometric template data
     */
    @PostMapping("/store-biometric-data")
    public ResponseEntity<?> storeBiometricData(@RequestBody Map<String, Object> request) {
        try {
            Integer empId = (Integer) request.get("empId");
            String terminalSerial = (String) request.get("terminalSerial");
            String biometricType = (String) request.get("biometricType"); // "fingerprint", "face", etc.
            String biometricTemplate = (String) request.get("biometricTemplate"); // Base64 encoded template
            Integer fingerIndex = (Integer) request.get("fingerIndex"); // 0-9 for finger index

            // Find employee mapping
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                return ResponseEntity.badRequest().body("Employee not registered on this terminal");
            }

            // Mark as enrolled and store biometric data reference
            mapping.setFingerprintEnrolled(true);
            mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.COMPLETED);
            mappingRepo.save(mapping);

            // Store biometric template (you can enhance this with a separate BiometricData
            // entity)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Biometric data stored successfully");
            response.put("empId", empId);
            response.put("biometricType", biometricType);
            response.put("fingerIndex", fingerIndex);
            response.put("templateSize", biometricTemplate != null ? biometricTemplate.length() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error storing biometric data: " + e.getMessage());
        }
    }

    /**
     * Get employee biometric status
     */
    @GetMapping("/biometric-status/{empId}")
    public ResponseEntity<?> getBiometricStatus(@PathVariable Integer empId) {
        try {
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            List<EmployeeDeviceMapping> mappings = mappingRepo.findByHrmEmployeeId(empId);

            Map<String, Object> status = new HashMap<>();
            status.put("empId", empId);
            status.put("fullName", employee.getFullName());
            status.put("registeredTerminals", mappings.size());
            status.put("fingerprintEnrolled",
                    mappings.stream().anyMatch(EmployeeDeviceMapping::getFingerprintEnrolled));

            List<Map<String, Object>> terminalStatus = mappings.stream().map(mapping -> {
                Map<String, Object> terminalInfo = new HashMap<>();
                terminalInfo.put("terminalSerial", mapping.getTerminalSerial());
                terminalInfo.put("machineEmployeeId", mapping.getEasytimeEmployeeId());
                terminalInfo.put("empCode", mapping.getEmpCode());
                terminalInfo.put("fingerprintEnrolled", mapping.getFingerprintEnrolled());
                terminalInfo.put("enrollmentStatus", mapping.getEnrollmentStatus());
                return terminalInfo;
            }).toList();

            status.put("terminals", terminalStatus);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting biometric status: " + e.getMessage());
        }
    }

    /**
     * Confirm fingerprint enrollment manually
     */
    @PostMapping("/confirm-enrollment-manual")
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
}
