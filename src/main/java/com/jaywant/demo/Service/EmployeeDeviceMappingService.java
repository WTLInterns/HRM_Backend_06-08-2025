package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeDeviceMappingService {

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SubAdminRepo subAdminRepo;

    /**
     * Automatically create device mapping for a single employee
     */
    @Transactional
    public EmployeeDeviceMapping createEmployeeDeviceMapping(Employee employee, String deviceSerialNumber) {
        if (deviceSerialNumber == null || deviceSerialNumber.trim().isEmpty()) {
            System.out.println("Device serial number is null or empty");
            return null; // No device serial number provided
        }

        // Validate employee data
        if (employee == null) {
            System.out.println("Employee is null");
            return null;
        }

        if (employee.getEmpId() <= 0) {
            System.out.println(
                    "Employee ID is 0 or negative for employee: " + employee.getFirstName() + " "
                            + employee.getLastName() +
                            ". Employee may not be saved to database yet.");
            return null;
        }

        if (employee.getSubadmin() == null) {
            System.out.println("Employee subadmin is null for employee ID: " + employee.getEmpId());
            return null;
        }

        System.out.println("Creating device mapping for Employee ID: " + employee.getEmpId() +
                ", Device: " + deviceSerialNumber +
                ", Subadmin ID: " + employee.getSubadmin().getId());

        // Check if mapping already exists
        Optional<EmployeeDeviceMapping> existingMapping = mappingRepo
                .findByHrmEmployeeIdAndTerminalSerial(employee.getEmpId(), deviceSerialNumber);

        if (existingMapping.isPresent()) {
            System.out.println("Mapping already exists for Employee ID: " + employee.getEmpId() +
                    " with device: " + deviceSerialNumber);
            // Update the existing mapping to ensure it's current
            EmployeeDeviceMapping existing = existingMapping.get();
            existing.setUpdatedAt(java.time.LocalDateTime.now());
            return mappingRepo.save(existing);
        }

        // Use HRM Employee ID as EasyTime Employee ID for direct mapping
        Integer easytimeEmployeeId = employee.getEmpId();

        // Check if this EasyTime Employee ID is already used on this device by another
        // HRM employee
        Optional<EmployeeDeviceMapping> conflictMapping = mappingRepo
                .findByEasytimeEmployeeIdAndTerminalSerial(easytimeEmployeeId, deviceSerialNumber);

        if (conflictMapping.isPresent() && !conflictMapping.get().getHrmEmployeeId().equals(employee.getEmpId())) {
            System.out.println("WARNING: EasyTime Employee ID " + easytimeEmployeeId +
                    " is already used by HRM Employee ID " + conflictMapping.get().getHrmEmployeeId() +
                    " on device " + deviceSerialNumber);
            // In case of conflict, use the next available ID
            easytimeEmployeeId = getNextEasyTimeEmployeeId(deviceSerialNumber);
            System.out.println("Using next available EasyTime ID: " + easytimeEmployeeId);
        } else {
            System.out.println("Using HRM Employee ID as EasyTime ID: " + easytimeEmployeeId);
        }

        // Generate employee code
        String empCode = generateEmployeeCode(employee, deviceSerialNumber);
        System.out.println("Generated employee code: " + empCode);

        // Create new mapping
        EmployeeDeviceMapping mapping = new EmployeeDeviceMapping(
                employee.getEmpId(),
                employee.getSubadmin().getId(),
                easytimeEmployeeId,
                deviceSerialNumber,
                empCode);

        mapping.setEnrollmentStatus(EmployeeDeviceMapping.EnrollmentStatus.PENDING);
        mapping.setFingerprintEnrolled(false);

        EmployeeDeviceMapping savedMapping = mappingRepo.save(mapping);
        System.out.println("Successfully created mapping with ID: " + savedMapping.getId());

        return savedMapping;
    }

    /**
     * Automatically map all employees of a subadmin to their device
     */
    @Transactional
    public void mapAllSubadminEmployeesToDevice(Integer subadminId, String deviceSerialNumber) {
        if (deviceSerialNumber == null || deviceSerialNumber.trim().isEmpty()) {
            System.out.println("Device serial number is null or empty for subadmin: " + subadminId);
            return; // No device serial number provided
        }

        System.out.println("=== Mapping all employees for Subadmin ID: " + subadminId + " to Device: "
                + deviceSerialNumber + " ===");

        // Get all employees for this subadmin
        List<Employee> employees = employeeRepo.findBySubadminId(subadminId);
        System.out.println("Found " + employees.size() + " employees for subadmin " + subadminId);

        for (Employee employee : employees) {
            System.out.println("Processing employee: " + employee.getFirstName() + " " + employee.getLastName() +
                    " (ID: " + employee.getEmpId() + ")");

            // Update employee's device serial number if not already set
            if (employee.getDeviceSerialNumber() == null ||
                    !employee.getDeviceSerialNumber().equals(deviceSerialNumber)) {
                employee.setDeviceSerialNumber(deviceSerialNumber);
                employeeRepo.save(employee);
                System.out.println("Updated device serial for employee: " + employee.getEmpId());
            }

            // Create or update device mapping (this method handles existing mappings
            // safely)
            createEmployeeDeviceMapping(employee, deviceSerialNumber);
        }

        System.out.println("=== Completed mapping for Subadmin ID: " + subadminId + " ===");
    }

    /**
     * Update device mapping when subadmin's device serial number changes
     */
    @Transactional
    public void updateSubadminDeviceMapping(Integer subadminId, String oldDeviceSerial, String newDeviceSerial) {
        System.out.println("=== Device Mapping Update ===");
        System.out.println("SubAdmin ID: " + subadminId);
        System.out.println("Old Device Serial: " + oldDeviceSerial);
        System.out.println("New Device Serial: " + newDeviceSerial);

        // Handle case where device serial is being removed
        if (newDeviceSerial == null || newDeviceSerial.trim().isEmpty()) {
            System.out.println("Removing device serial - deleting all mappings");
            if (oldDeviceSerial != null && !oldDeviceSerial.trim().isEmpty()) {
                List<EmployeeDeviceMapping> oldMappings = mappingRepo
                        .findBySubadminIdAndTerminalSerial(subadminId, oldDeviceSerial);
                System.out.println("Found " + oldMappings.size() + " old mappings to delete");
                mappingRepo.deleteAll(oldMappings);
            }
            return;
        }

        // Handle case where device serial number is being changed
        if (oldDeviceSerial != null && !oldDeviceSerial.equals(newDeviceSerial)) {
            System.out.println("Device serial changed - updating mappings");
            // Delete old mappings
            List<EmployeeDeviceMapping> oldMappings = mappingRepo
                    .findBySubadminIdAndTerminalSerial(subadminId, oldDeviceSerial);
            System.out.println("Found " + oldMappings.size() + " old mappings to delete");
            mappingRepo.deleteAll(oldMappings);
        }

        // Handle case where device serial is being added for the first time or changed
        if (!newDeviceSerial.equals(oldDeviceSerial)) {
            System.out.println("Creating new mappings for device: " + newDeviceSerial);
            mapAllSubadminEmployeesToDevice(subadminId, newDeviceSerial);
        } else {
            System.out.println("Device serial unchanged - no mapping updates needed");
        }

        System.out.println("=== Device Mapping Update Complete ===");
    }

    /**
     * Handle new subadmin creation with device mapping
     */
    @Transactional
    public void handleNewSubadminDeviceMapping(Subadmin subadmin) {
        if (subadmin.getDeviceSerialNumber() != null && !subadmin.getDeviceSerialNumber().trim().isEmpty()) {
            mapAllSubadminEmployeesToDevice(subadmin.getId(), subadmin.getDeviceSerialNumber());
        }
    }

    /**
     * Handle new employee creation with automatic device mapping
     */
    @Transactional
    public EmployeeDeviceMapping handleNewEmployeeDeviceMapping(Employee employee) {
        if (employee.getSubadmin() != null &&
                employee.getSubadmin().getDeviceSerialNumber() != null &&
                !employee.getSubadmin().getDeviceSerialNumber().trim().isEmpty()) {

            return createEmployeeDeviceMapping(employee, employee.getSubadmin().getDeviceSerialNumber());
        }
        return null;
    }

    /**
     * Get next available EasyTime employee ID for a device
     */
    private Integer getNextEasyTimeEmployeeId(String deviceSerialNumber) {
        Integer nextId = mappingRepo.findNextEasytimeEmployeeId(deviceSerialNumber);
        return nextId != null ? nextId : 1;
    }

    /**
     * Generate unique employee code
     */
    private String generateEmployeeCode(Employee employee, String deviceSerialNumber) {
        return String.format("SA%d_EMP%d_%s_%03d",
                employee.getSubadmin().getId(),
                employee.getEmpId(),
                deviceSerialNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase(),
                System.currentTimeMillis() % 1000);
    }

    /**
     * Get all mappings for a subadmin
     */
    public List<EmployeeDeviceMapping> getSubadminMappings(Integer subadminId) {
        return mappingRepo.findBySubadminIdWithEmployeeDetails(subadminId);
    }

    /**
     * Get all mappings for a device
     */
    public List<EmployeeDeviceMapping> getDeviceMappings(String deviceSerialNumber) {
        return mappingRepo.findByTerminalSerial(deviceSerialNumber);
    }

    /**
     * Check if employee is already mapped to a device
     */
    public boolean isEmployeeMappedToDevice(Integer employeeId, String deviceSerialNumber) {
        return mappingRepo.findByHrmEmployeeIdAndTerminalSerial(employeeId, deviceSerialNumber).isPresent();
    }

    /**
     * Remove employee from device mapping
     */
    @Transactional
    public void removeEmployeeFromDevice(Integer employeeId, String deviceSerialNumber) {
        Optional<EmployeeDeviceMapping> mapping = mappingRepo
                .findByHrmEmployeeIdAndTerminalSerial(employeeId, deviceSerialNumber);
        mapping.ifPresent(mappingRepo::delete);
    }

    /**
     * Remove all mappings for a device
     */
    @Transactional
    public void removeAllDeviceMappings(String deviceSerialNumber) {
        mappingRepo.deleteByTerminalSerial(deviceSerialNumber);
    }

    /**
     * Get all mappings for an employee
     */
    public List<EmployeeDeviceMapping> getEmployeeMappings(Integer employeeId) {
        return mappingRepo.findByHrmEmployeeId(employeeId);
    }

    /**
     * Nullify employee reference in device mappings (keeps mapping but removes
     * employee link)
     */
    @Transactional
    public void nullifyEmployeeReference(Integer employeeId) {
        List<EmployeeDeviceMapping> mappings = mappingRepo.findByHrmEmployeeId(employeeId);
        for (EmployeeDeviceMapping mapping : mappings) {
            // The employee relationship will be automatically nullified when employee is
            // deleted
            // due to the foreign key constraint. We don't need to do anything here.
            System.out.println("Device mapping ID " + mapping.getId() + " will have employee reference nullified");
        }
    }

    /**
     * Remove all mappings for an employee
     */
    @Transactional
    public void removeAllEmployeeMappings(Integer employeeId) {
        mappingRepo.deleteByHrmEmployeeId(employeeId);
    }

    /**
     * Recreate all mappings for a subadmin with correct EasyTime Employee IDs
     */
    @Transactional
    public void recreateSubadminMappingsWithCorrectIds(Integer subadminId, String deviceSerialNumber) {
        System.out.println("=== Recreating mappings for Subadmin ID: " + subadminId + " with correct EasyTime IDs ===");

        // Delete all existing mappings for this subadmin and device
        List<EmployeeDeviceMapping> existingMappings = mappingRepo
                .findBySubadminIdAndTerminalSerial(subadminId, deviceSerialNumber);
        System.out.println("Deleting " + existingMappings.size() + " existing mappings");
        mappingRepo.deleteAll(existingMappings);

        // Recreate mappings with correct EasyTime Employee IDs
        mapAllSubadminEmployeesToDevice(subadminId, deviceSerialNumber);

        System.out.println("=== Completed recreating mappings for Subadmin ID: " + subadminId + " ===");
    }
}
