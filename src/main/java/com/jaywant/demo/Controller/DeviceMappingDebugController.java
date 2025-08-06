package com.jaywant.demo.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.EmployeeDeviceMappingService;

@RestController
@RequestMapping("/api/debug/device-mapping")
public class DeviceMappingDebugController {

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SubAdminRepo subAdminRepo;

    @Autowired
    private EmployeeDeviceMappingService deviceMappingService;

    /**
     * Debug: Check device mapping status for a subadmin
     */
    @GetMapping("/subadmin/{subadminId}/status")
    public ResponseEntity<?> getSubadminMappingStatus(@PathVariable Integer subadminId) {
        try {
            Map<String, Object> status = new HashMap<>();

            // Get subadmin details
            Subadmin subadmin = subAdminRepo.findById(subadminId).orElse(null);
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found with ID: " + subadminId);
            }

            status.put("subadminId", subadminId);
            status.put("subadminName", subadmin.getName() + " " + subadmin.getLastname());
            status.put("deviceSerialNumber", subadmin.getDeviceSerialNumber());
            status.put("hasDeviceSerial",
                    subadmin.getDeviceSerialNumber() != null && !subadmin.getDeviceSerialNumber().trim().isEmpty());

            // Get all employees for this subadmin
            List<Employee> employees = employeeRepo.findBySubadminId(subadminId);
            status.put("totalEmployees", employees.size());

            // Get all device mappings for this subadmin
            List<EmployeeDeviceMapping> mappings = mappingRepo.findBySubadminId(subadminId);
            status.put("totalMappings", mappings.size());

            // Check each employee's device serial number
            Map<String, Object> employeeDetails = new HashMap<>();
            for (Employee emp : employees) {
                Map<String, Object> empInfo = new HashMap<>();
                empInfo.put("empId", emp.getEmpId());
                empInfo.put("name", emp.getFirstName() + " " + emp.getLastName());
                empInfo.put("deviceSerialNumber", emp.getDeviceSerialNumber());
                empInfo.put("hasDeviceSerial",
                        emp.getDeviceSerialNumber() != null && !emp.getDeviceSerialNumber().trim().isEmpty());

                // Check if this employee has device mapping
                List<EmployeeDeviceMapping> empMappings = mappingRepo.findByHrmEmployeeId(emp.getEmpId());
                empInfo.put("mappingCount", empMappings.size());
                empInfo.put("hasMappings", !empMappings.isEmpty());

                employeeDetails.put("employee_" + emp.getEmpId(), empInfo);
            }
            status.put("employeeDetails", employeeDetails);

            // Summary
            long employeesWithDeviceSerial = employees.stream()
                    .filter(emp -> emp.getDeviceSerialNumber() != null && !emp.getDeviceSerialNumber().trim().isEmpty())
                    .count();

            status.put("summary", Map.of(
                    "employeesWithDeviceSerial", employeesWithDeviceSerial,
                    "employeesWithoutDeviceSerial", employees.size() - employeesWithDeviceSerial,
                    "mappingCoverage", mappings.size() + "/" + employees.size()));

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Debug: Check specific employee mapping
     */
    @GetMapping("/employee/{empId}/status")
    public ResponseEntity<?> getEmployeeMappingStatus(@PathVariable Integer empId) {
        try {
            Map<String, Object> status = new HashMap<>();

            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found with ID: " + empId);
            }

            status.put("empId", empId);
            status.put("name", employee.getFirstName() + " " + employee.getLastName());
            status.put("deviceSerialNumber", employee.getDeviceSerialNumber());
            status.put("subadminId", employee.getSubadmin() != null ? employee.getSubadmin().getId() : null);

            if (employee.getSubadmin() != null) {
                status.put("subadminDeviceSerial", employee.getSubadmin().getDeviceSerialNumber());
                status.put("deviceSerialMatch",
                        employee.getDeviceSerialNumber() != null &&
                                employee.getDeviceSerialNumber()
                                        .equals(employee.getSubadmin().getDeviceSerialNumber()));
            }

            // Get mappings for this employee
            List<EmployeeDeviceMapping> mappings = mappingRepo.findByHrmEmployeeId(empId);
            status.put("mappings", mappings);
            status.put("mappingCount", mappings.size());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Debug: Manually trigger device mapping for an employee
     */
    @PostMapping("/employee/{empId}/create-mapping")
    public ResponseEntity<?> createEmployeeMapping(@PathVariable Integer empId) {
        try {
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found with ID: " + empId);
            }

            if (employee.getSubadmin() == null) {
                return ResponseEntity.badRequest().body("Employee has no subadmin assigned");
            }

            String deviceSerial = employee.getSubadmin().getDeviceSerialNumber();
            if (deviceSerial == null || deviceSerial.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Subadmin has no device serial number assigned");
            }

            // Update employee's device serial number if not set
            if (employee.getDeviceSerialNumber() == null || !employee.getDeviceSerialNumber().equals(deviceSerial)) {
                employee.setDeviceSerialNumber(deviceSerial);
                employeeRepo.save(employee);
            }

            // Create device mapping
            EmployeeDeviceMapping mapping = deviceMappingService.handleNewEmployeeDeviceMapping(employee);

            Map<String, Object> result = new HashMap<>();
            result.put("success", mapping != null);
            result.put("empId", empId);
            result.put("deviceSerial", deviceSerial);
            result.put("mapping", mapping);
            result.put("message", mapping != null ? "Mapping created successfully" : "Failed to create mapping");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Debug: Fix all employee mappings for a subadmin
     */
    @PostMapping("/subadmin/{subadminId}/fix-mappings")
    public ResponseEntity<?> fixSubadminMappings(@PathVariable Integer subadminId) {
        try {
            Subadmin subadmin = subAdminRepo.findById(subadminId).orElse(null);
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found with ID: " + subadminId);
            }

            String deviceSerial = subadmin.getDeviceSerialNumber();
            if (deviceSerial == null || deviceSerial.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Subadmin has no device serial number assigned");
            }

            List<Employee> employees = employeeRepo.findBySubadminId(subadminId);
            int fixed = 0;
            int created = 0;

            for (Employee employee : employees) {
                // Update employee's device serial number
                if (employee.getDeviceSerialNumber() == null
                        || !employee.getDeviceSerialNumber().equals(deviceSerial)) {
                    employee.setDeviceSerialNumber(deviceSerial);
                    employeeRepo.save(employee);
                    fixed++;
                }

                // Check if mapping exists
                Optional<EmployeeDeviceMapping> existingMapping = mappingRepo.findByHrmEmployeeIdAndTerminalSerial(
                        employee.getEmpId(), deviceSerial);

                if (existingMapping.isEmpty()) {
                    // Create new mapping
                    EmployeeDeviceMapping mapping = deviceMappingService.createEmployeeDeviceMapping(employee,
                            deviceSerial);
                    if (mapping != null) {
                        created++;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("subadminId", subadminId);
            result.put("deviceSerial", deviceSerial);
            result.put("totalEmployees", employees.size());
            result.put("employeesFixed", fixed);
            result.put("mappingsCreated", created);
            result.put("message", "Fixed " + fixed + " employees and created " + created + " mappings");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Debug: Get all mappings for a device serial
     */
    @GetMapping("/device/{deviceSerial}/mappings")
    public ResponseEntity<?> getDeviceMappings(@PathVariable String deviceSerial) {
        try {
            List<EmployeeDeviceMapping> mappings = mappingRepo.findByTerminalSerial(deviceSerial);

            Map<String, Object> result = new HashMap<>();
            result.put("deviceSerial", deviceSerial);
            result.put("totalMappings", mappings.size());
            result.put("mappings", mappings);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
