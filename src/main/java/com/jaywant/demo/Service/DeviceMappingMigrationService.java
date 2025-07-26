package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeviceMappingMigrationService {

    @Autowired
    private SubAdminRepo subAdminRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private EmployeeDeviceMappingService deviceMappingService;

    /**
     * Migrate all existing subadmins and employees to device mapping system
     * This should be run once after implementing the device mapping feature
     */
    @Transactional
    public MigrationResult migrateExistingData() {
        MigrationResult result = new MigrationResult();
        
        try {
            // Get all subadmins
            List<Subadmin> allSubadmins = subAdminRepo.findAll();
            result.totalSubadmins = allSubadmins.size();

            for (Subadmin subadmin : allSubadmins) {
                try {
                    if (subadmin.getDeviceSerialNumber() != null && 
                        !subadmin.getDeviceSerialNumber().trim().isEmpty()) {
                        
                        // Map all employees of this subadmin to the device
                        deviceMappingService.mapAllSubadminEmployeesToDevice(
                            subadmin.getId(), subadmin.getDeviceSerialNumber());
                        
                        result.processedSubadmins++;
                        
                        // Count employees processed
                        List<Employee> employees = employeeRepo.findBySubadminId(subadmin.getId());
                        result.totalEmployees += employees.size();
                        result.processedEmployees += employees.size();
                        
                    } else {
                        result.skippedSubadmins++;
                        
                        // Count employees skipped
                        List<Employee> employees = employeeRepo.findBySubadminId(subadmin.getId());
                        result.totalEmployees += employees.size();
                        result.skippedEmployees += employees.size();
                    }
                } catch (Exception e) {
                    result.errorSubadmins++;
                    result.errors.add("Error processing subadmin " + subadmin.getId() + ": " + e.getMessage());
                }
            }

            result.success = true;
            result.message = "Migration completed successfully";

        } catch (Exception e) {
            result.success = false;
            result.message = "Migration failed: " + e.getMessage();
            result.errors.add(e.getMessage());
        }

        return result;
    }

    /**
     * Migrate specific subadmin and its employees
     */
    @Transactional
    public MigrationResult migrateSubadmin(Integer subadminId) {
        MigrationResult result = new MigrationResult();
        
        try {
            Subadmin subadmin = subAdminRepo.findById(subadminId).orElse(null);
            if (subadmin == null) {
                result.success = false;
                result.message = "Subadmin not found with ID: " + subadminId;
                return result;
            }

            result.totalSubadmins = 1;

            if (subadmin.getDeviceSerialNumber() != null && 
                !subadmin.getDeviceSerialNumber().trim().isEmpty()) {
                
                deviceMappingService.mapAllSubadminEmployeesToDevice(
                    subadmin.getId(), subadmin.getDeviceSerialNumber());
                
                result.processedSubadmins = 1;
                
                List<Employee> employees = employeeRepo.findBySubadminId(subadmin.getId());
                result.totalEmployees = employees.size();
                result.processedEmployees = employees.size();
                
                result.success = true;
                result.message = "Subadmin migration completed successfully";
                
            } else {
                result.skippedSubadmins = 1;
                result.success = false;
                result.message = "Subadmin has no device serial number configured";
            }

        } catch (Exception e) {
            result.success = false;
            result.message = "Migration failed: " + e.getMessage();
            result.errors.add(e.getMessage());
        }

        return result;
    }

    /**
     * Check migration status
     */
    public MigrationStatus checkMigrationStatus() {
        MigrationStatus status = new MigrationStatus();
        
        try {
            List<Subadmin> allSubadmins = subAdminRepo.findAll();
            status.totalSubadmins = allSubadmins.size();

            for (Subadmin subadmin : allSubadmins) {
                if (subadmin.getDeviceSerialNumber() != null && 
                    !subadmin.getDeviceSerialNumber().trim().isEmpty()) {
                    status.subadminsWithDevices++;
                    
                    List<Employee> employees = employeeRepo.findBySubadminId(subadmin.getId());
                    status.totalEmployees += employees.size();
                    
                    // Check how many employees are actually mapped
                    for (Employee emp : employees) {
                        if (deviceMappingService.isEmployeeMappedToDevice(
                            emp.getEmpId(), subadmin.getDeviceSerialNumber())) {
                            status.mappedEmployees++;
                        } else {
                            status.unmappedEmployees++;
                        }
                    }
                } else {
                    status.subadminsWithoutDevices++;
                    List<Employee> employees = employeeRepo.findBySubadminId(subadmin.getId());
                    status.totalEmployees += employees.size();
                    status.unmappedEmployees += employees.size();
                }
            }

        } catch (Exception e) {
            status.error = e.getMessage();
        }

        return status;
    }

    // Result classes
    public static class MigrationResult {
        public boolean success = false;
        public String message = "";
        public int totalSubadmins = 0;
        public int processedSubadmins = 0;
        public int skippedSubadmins = 0;
        public int errorSubadmins = 0;
        public int totalEmployees = 0;
        public int processedEmployees = 0;
        public int skippedEmployees = 0;
        public java.util.List<String> errors = new java.util.ArrayList<>();
    }

    public static class MigrationStatus {
        public int totalSubadmins = 0;
        public int subadminsWithDevices = 0;
        public int subadminsWithoutDevices = 0;
        public int totalEmployees = 0;
        public int mappedEmployees = 0;
        public int unmappedEmployees = 0;
        public String error = null;
    }
}
