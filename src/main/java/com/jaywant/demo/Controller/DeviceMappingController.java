package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.EmployeeDeviceMappingService;
import com.jaywant.demo.Service.DeviceMappingMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/device-mapping")
@CrossOrigin(origins = "*")
public class DeviceMappingController {

    @Autowired
    private EmployeeDeviceMappingService deviceMappingService;

    @Autowired
    private SubAdminRepo subAdminRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private DeviceMappingMigrationService migrationService;

    /**
     * Manually trigger device mapping for all employees of a subadmin
     */
    @PostMapping("/subadmin/{subadminId}/map-all")
    public ResponseEntity<?> mapAllEmployeesToDevice(@PathVariable Integer subadminId) {
        try {
            Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
            if (!subadminOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Subadmin not found with ID: " + subadminId);
            }

            Subadmin subadmin = subadminOpt.get();
            if (subadmin.getDeviceSerialNumber() == null || subadmin.getDeviceSerialNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No device serial number configured for this subadmin");
            }

            deviceMappingService.mapAllSubadminEmployeesToDevice(subadminId, subadmin.getDeviceSerialNumber());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All employees mapped to device successfully");
            response.put("subadminId", subadminId);
            response.put("deviceSerialNumber", subadmin.getDeviceSerialNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all device mappings for a subadmin
     */
    @GetMapping("/subadmin/{subadminId}")
    public ResponseEntity<?> getSubadminMappings(@PathVariable Integer subadminId) {
        try {
            List<EmployeeDeviceMapping> mappings = deviceMappingService.getSubadminMappings(subadminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mappings", mappings);
            response.put("count", mappings.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all device mappings for a specific device
     */
    @GetMapping("/device/{deviceSerialNumber}")
    public ResponseEntity<?> getDeviceMappings(@PathVariable String deviceSerialNumber) {
        try {
            List<EmployeeDeviceMapping> mappings = deviceMappingService.getDeviceMappings(deviceSerialNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mappings", mappings);
            response.put("count", mappings.size());
            response.put("deviceSerialNumber", deviceSerialNumber);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Remove employee from device mapping
     */
    @DeleteMapping("/employee/{employeeId}/device/{deviceSerialNumber}")
    public ResponseEntity<?> removeEmployeeFromDevice(
            @PathVariable Integer employeeId,
            @PathVariable String deviceSerialNumber) {
        try {
            deviceMappingService.removeEmployeeFromDevice(employeeId, deviceSerialNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee removed from device mapping");
            response.put("employeeId", employeeId);
            response.put("deviceSerialNumber", deviceSerialNumber);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Trigger mapping for all existing subadmins that have device serial numbers
     */
    @PostMapping("/migrate-all")
    public ResponseEntity<?> migrateAllExistingData() {
        try {
            List<Subadmin> allSubadmins = subAdminRepo.findAll();
            int processedCount = 0;
            int skippedCount = 0;

            for (Subadmin subadmin : allSubadmins) {
                if (subadmin.getDeviceSerialNumber() != null && !subadmin.getDeviceSerialNumber().trim().isEmpty()) {
                    deviceMappingService.mapAllSubadminEmployeesToDevice(
                            subadmin.getId(), subadmin.getDeviceSerialNumber());
                    processedCount++;
                } else {
                    skippedCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Migration completed");
            response.put("totalSubadmins", allSubadmins.size());
            response.put("processedSubadmins", processedCount);
            response.put("skippedSubadmins", skippedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if employee is mapped to a device
     */
    @GetMapping("/employee/{employeeId}/device/{deviceSerialNumber}/check")
    public ResponseEntity<?> checkEmployeeMapping(
            @PathVariable Integer employeeId,
            @PathVariable String deviceSerialNumber) {
        try {
            boolean isMapped = deviceMappingService.isEmployeeMappedToDevice(employeeId, deviceSerialNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isMapped", isMapped);
            response.put("employeeId", employeeId);
            response.put("deviceSerialNumber", deviceSerialNumber);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Run complete migration for all existing data
     */
    @PostMapping("/migration/run-all")
    public ResponseEntity<?> runCompleteMigration() {
        try {
            DeviceMappingMigrationService.MigrationResult result = migrationService.migrateExistingData();

            if (result.success) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check migration status
     */
    @GetMapping("/migration/status")
    public ResponseEntity<?> getMigrationStatus() {
        try {
            DeviceMappingMigrationService.MigrationStatus status = migrationService.checkMigrationStatus();
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Migrate specific subadmin
     */
    @PostMapping("/migration/subadmin/{subadminId}")
    public ResponseEntity<?> migrateSpecificSubadmin(@PathVariable Integer subadminId) {
        try {
            DeviceMappingMigrationService.MigrationResult result = migrationService.migrateSubadmin(subadminId);

            if (result.success) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Test endpoint to verify device mapping workflow
     */
    @GetMapping("/test/workflow/{subadminId}")
    public ResponseEntity<?> testDeviceMappingWorkflow(@PathVariable Integer subadminId) {
        try {
            Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
            if (!subadminOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Subadmin not found with ID: " + subadminId);
            }

            Subadmin subadmin = subadminOpt.get();
            List<EmployeeDeviceMapping> mappings = deviceMappingService.getSubadminMappings(subadminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subadminId", subadminId);
            response.put("subadminName", subadmin.getName());
            response.put("deviceSerialNumber", subadmin.getDeviceSerialNumber());
            response.put("totalMappings", mappings.size());
            response.put("mappings", mappings);

            // Additional verification
            if (subadmin.getDeviceSerialNumber() != null) {
                List<EmployeeDeviceMapping> deviceMappings = deviceMappingService
                        .getDeviceMappings(subadmin.getDeviceSerialNumber());
                response.put("deviceMappingsCount", deviceMappings.size());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Debug endpoint to check employee data for a subadmin
     */
    @GetMapping("/debug/subadmin/{subadminId}/employees")
    public ResponseEntity<?> debugSubadminEmployees(@PathVariable Integer subadminId) {
        try {
            Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
            if (!subadminOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Subadmin not found with ID: " + subadminId);
            }

            Subadmin subadmin = subadminOpt.get();

            // Get employees using the repository method
            List<Employee> employees = employeeRepo.findBySubadminId(subadminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subadminId", subadminId);
            response.put("subadminName", subadmin.getName());
            response.put("deviceSerialNumber", subadmin.getDeviceSerialNumber());
            response.put("employeeCount", employees.size());

            List<Map<String, Object>> employeeDetails = new ArrayList<>();
            for (Employee emp : employees) {
                Map<String, Object> empData = new HashMap<>();
                empData.put("empId", emp.getEmpId());
                empData.put("firstName", emp.getFirstName());
                empData.put("lastName", emp.getLastName());
                empData.put("email", emp.getEmail());
                empData.put("hasSubadmin", emp.getSubadmin() != null);
                if (emp.getSubadmin() != null) {
                    empData.put("subadminId", emp.getSubadmin().getId());
                    empData.put("subadminDeviceSerial", emp.getSubadmin().getDeviceSerialNumber());
                }
                employeeDetails.add(empData);
            }
            response.put("employees", employeeDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Show actual EmployeeDeviceMapping records in the database
     */
    @GetMapping("/debug/subadmin/{subadminId}/mappings")
    public ResponseEntity<?> debugActualMappings(@PathVariable Integer subadminId) {
        try {
            List<EmployeeDeviceMapping> mappings = deviceMappingService.getSubadminMappings(subadminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("subadminId", subadminId);
            response.put("totalMappings", mappings.size());

            List<Map<String, Object>> mappingDetails = new ArrayList<>();
            for (EmployeeDeviceMapping mapping : mappings) {
                Map<String, Object> mappingData = new HashMap<>();
                mappingData.put("id", mapping.getId());
                mappingData.put("hrmEmployeeId", mapping.getHrmEmployeeId());
                mappingData.put("subadminId", mapping.getSubadminId());
                mappingData.put("easytimeEmployeeId", mapping.getEasytimeEmployeeId());
                mappingData.put("terminalSerial", mapping.getTerminalSerial());
                mappingData.put("empCode", mapping.getEmpCode());
                mappingData.put("fingerprintEnrolled", mapping.getFingerprintEnrolled());
                mappingData.put("enrollmentStatus", mapping.getEnrollmentStatus());
                mappingData.put("createdAt", mapping.getCreatedAt());
                mappingData.put("updatedAt", mapping.getUpdatedAt());

                // Check if the employee relationship is loaded
                if (mapping.getEmployee() != null) {
                    mappingData.put("employeeName",
                            mapping.getEmployee().getFirstName() + " " + mapping.getEmployee().getLastName());
                } else {
                    mappingData.put("employeeName", "Employee relationship not loaded");
                }

                mappingDetails.add(mappingData);
            }
            response.put("mappings", mappingDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Recreate mappings with correct EasyTime Employee IDs (same as HRM Employee
     * IDs)
     */
    @PostMapping("/subadmin/{subadminId}/recreate-mappings")
    public ResponseEntity<?> recreateMappingsWithCorrectIds(@PathVariable Integer subadminId) {
        try {
            Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
            if (!subadminOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Subadmin not found with ID: " + subadminId);
            }

            Subadmin subadmin = subadminOpt.get();
            if (subadmin.getDeviceSerialNumber() == null || subadmin.getDeviceSerialNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("No device serial number configured for this subadmin");
            }

            deviceMappingService.recreateSubadminMappingsWithCorrectIds(subadminId, subadmin.getDeviceSerialNumber());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All mappings recreated with correct EasyTime Employee IDs");
            response.put("subadminId", subadminId);
            response.put("deviceSerialNumber", subadmin.getDeviceSerialNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
