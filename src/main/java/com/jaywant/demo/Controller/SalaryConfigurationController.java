package com.jaywant.demo.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.SalaryConfiguration;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SalaryConfigurationRepository;
import com.jaywant.demo.Repo.SubAdminRepo;

@RestController
@RequestMapping("/api/salary-config/{subadminId}")
// @CrossOrigin(origins = "*")
public class SalaryConfigurationController {
    @Autowired
    private EmployeeRepo employeeRepo;

    // Get salary configuration for a specific employee
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getSalaryConfigForEmployee(
            @PathVariable Integer subadminId,
            @PathVariable Integer employeeId) {
        try {
            Optional<SalaryConfiguration> config = salaryConfigRepository.findByEmployee_EmpId(employeeId);
            if (config.isPresent()) {
                return ResponseEntity.ok(config.get());
            } else {
                return ResponseEntity.status(404).body("Salary configuration not found for employee");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching salary configuration: " + e.getMessage());
        }
    }

    // Set salary for a single employee
    @PostMapping("/employee/{employeeId}")
    public ResponseEntity<?> setSalaryForEmployee(
            @PathVariable Integer subadminId,
            @PathVariable Integer employeeId,
            @RequestBody SalaryConfiguration configData) {
        try {
            Employee employee = employeeRepo.findById(employeeId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }
            Subadmin subadmin = employee.getSubadmin();
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found for employee");
            }
            // Check if configuration already exists for this employee
            Optional<SalaryConfiguration> existingConfig = salaryConfigRepository.findByEmployee_EmpId(employeeId);
            SalaryConfiguration config;
            if (existingConfig.isPresent()) {
                config = existingConfig.get();
                updateConfigurationFields(config, configData);
            } else {
                config = new SalaryConfiguration(subadmin, employee);
                updateConfigurationFields(config, configData);
            }
            if (!validateConfiguration(config)) {
                return ResponseEntity.badRequest().body("Invalid configuration: Percentages must be between 0 and 100");
            }
            SalaryConfiguration savedConfig = salaryConfigRepository.save(config);
            return ResponseEntity.ok(Map.of("success", true, "configuration", savedConfig));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving salary configuration: " + e.getMessage());
        }
    }

    // Set salary for multiple employees (batch)
    @PostMapping("/employees")
    public ResponseEntity<?> setSalaryForMultipleEmployees(
            @PathVariable Integer subadminId,
            @RequestBody Map<String, Object> payload) {
        try {
            // Expect payload: { employeeIds: [1,2,3], configData: {...} }
            Object empIdsObj = payload.get("employeeIds");
            Object configObj = payload.get("configData");
            if (!(empIdsObj instanceof java.util.List) || configObj == null) {
                return ResponseEntity.badRequest().body("Invalid payload");
            }
            java.util.List<Integer> employeeIds = (java.util.List<Integer>) empIdsObj;
            SalaryConfiguration configData = new SalaryConfiguration();
            // You may need to map configObj to SalaryConfiguration fields manually
            // For now, assume configObj is a Map<String, Object>
            Map<String, Object> configMap = (Map<String, Object>) configObj;
            // Set fields from configMap
            if (configMap.get("basicPercentage") != null)
                configData.setBasicPercentage(Double.valueOf(configMap.get("basicPercentage").toString()));
            if (configMap.get("hraPercentage") != null)
                configData.setHraPercentage(Double.valueOf(configMap.get("hraPercentage").toString()));
            if (configMap.get("daPercentage") != null)
                configData.setDaPercentage(Double.valueOf(configMap.get("daPercentage").toString()));
            if (configMap.get("specialAllowancePercentage") != null)
                configData.setSpecialAllowancePercentage(
                        Double.valueOf(configMap.get("specialAllowancePercentage").toString()));
            if (configMap.get("professionalTax") != null)
                configData.setProfessionalTax(Double.valueOf(configMap.get("professionalTax").toString()));
            if (configMap.get("tdsPercentage") != null)
                configData.setTdsPercentage(Double.valueOf(configMap.get("tdsPercentage").toString()));
            if (configMap.get("transportAllowance") != null)
                configData.setTransportAllowance(Double.valueOf(configMap.get("transportAllowance").toString()));
            if (configMap.get("medicalAllowance") != null)
                configData.setMedicalAllowance(Double.valueOf(configMap.get("medicalAllowance").toString()));
            if (configMap.get("foodAllowance") != null)
                configData.setFoodAllowance(Double.valueOf(configMap.get("foodAllowance").toString()));
            if (configMap.get("pfPercentage") != null)
                configData.setPfPercentage(Double.valueOf(configMap.get("pfPercentage").toString()));
            if (configMap.get("esiPercentage") != null)
                configData.setEsiPercentage(Double.valueOf(configMap.get("esiPercentage").toString()));

            int successCount = 0;
            for (Integer empId : employeeIds) {
                Employee employee = employeeRepo.findById(empId).orElse(null);
                if (employee == null)
                    continue;
                Subadmin subadmin = employee.getSubadmin();
                if (subadmin == null)
                    continue;
                Optional<SalaryConfiguration> existingConfig = salaryConfigRepository.findByEmployee_EmpId(empId);
                SalaryConfiguration config;
                if (existingConfig.isPresent()) {
                    config = existingConfig.get();
                    updateConfigurationFields(config, configData);
                } else {
                    config = new SalaryConfiguration(subadmin, employee);
                    updateConfigurationFields(config, configData);
                }
                if (!validateConfiguration(config))
                    continue;
                salaryConfigRepository.save(config);
                successCount++;
            }
            return ResponseEntity.ok(Map.of("success", true, "updated", successCount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving salary configuration: " + e.getMessage());
        }
    }

    // Update salary for a single employee
    @PutMapping("/employee/{employeeId}")
    public ResponseEntity<?> updateSalaryForEmployee(
            @PathVariable Integer subadminId,
            @PathVariable Integer employeeId,
            @RequestBody SalaryConfiguration configData) {
        try {
            Employee employee = employeeRepo.findById(employeeId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }
            Subadmin subadmin = employee.getSubadmin();
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found for employee");
            }
            Optional<SalaryConfiguration> existingConfig = salaryConfigRepository.findByEmployee_EmpId(employeeId);
            if (existingConfig.isPresent()) {
                SalaryConfiguration config = existingConfig.get();
                updateConfigurationFields(config, configData);
                if (!validateConfiguration(config)) {
                    return ResponseEntity.badRequest()
                            .body("Invalid configuration: Percentages must be between 0 and 100");
                }
                SalaryConfiguration savedConfig = salaryConfigRepository.save(config);
                return ResponseEntity.ok(Map.of("success", true, "configuration", savedConfig));
            } else {
                return ResponseEntity.badRequest().body("Salary configuration not found for employee");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating salary configuration: " + e.getMessage());
        }
    }

    // Update salary for multiple employees (batch)
    @PutMapping("/employees")
    public ResponseEntity<?> updateSalaryForMultipleEmployees(
            @PathVariable Integer subadminId,
            @RequestBody Map<String, Object> payload) {
        try {
            Object empIdsObj = payload.get("employeeIds");
            Object configObj = payload.get("configData");
            if (!(empIdsObj instanceof java.util.List) || configObj == null) {
                return ResponseEntity.badRequest().body("Invalid payload");
            }
            java.util.List<Integer> employeeIds = (java.util.List<Integer>) empIdsObj;
            SalaryConfiguration configData = new SalaryConfiguration();
            Map<String, Object> configMap = (Map<String, Object>) configObj;
            if (configMap.get("basicPercentage") != null)
                configData.setBasicPercentage(Double.valueOf(configMap.get("basicPercentage").toString()));
            if (configMap.get("hraPercentage") != null)
                configData.setHraPercentage(Double.valueOf(configMap.get("hraPercentage").toString()));
            if (configMap.get("daPercentage") != null)
                configData.setDaPercentage(Double.valueOf(configMap.get("daPercentage").toString()));
            if (configMap.get("specialAllowancePercentage") != null)
                configData.setSpecialAllowancePercentage(
                        Double.valueOf(configMap.get("specialAllowancePercentage").toString()));
            if (configMap.get("professionalTax") != null)
                configData.setProfessionalTax(Double.valueOf(configMap.get("professionalTax").toString()));
            if (configMap.get("tdsPercentage") != null)
                configData.setTdsPercentage(Double.valueOf(configMap.get("tdsPercentage").toString()));
            if (configMap.get("transportAllowance") != null)
                configData.setTransportAllowance(Double.valueOf(configMap.get("transportAllowance").toString()));
            if (configMap.get("medicalAllowance") != null)
                configData.setMedicalAllowance(Double.valueOf(configMap.get("medicalAllowance").toString()));
            if (configMap.get("foodAllowance") != null)
                configData.setFoodAllowance(Double.valueOf(configMap.get("foodAllowance").toString()));
            if (configMap.get("pfPercentage") != null)
                configData.setPfPercentage(Double.valueOf(configMap.get("pfPercentage").toString()));
            if (configMap.get("esiPercentage") != null)
                configData.setEsiPercentage(Double.valueOf(configMap.get("esiPercentage").toString()));

            int successCount = 0;
            for (Integer empId : employeeIds) {
                Employee employee = employeeRepo.findById(empId).orElse(null);
                if (employee == null)
                    continue;
                Subadmin subadmin = employee.getSubadmin();
                if (subadmin == null)
                    continue;
                Optional<SalaryConfiguration> existingConfig = salaryConfigRepository.findByEmployee_EmpId(empId);
                SalaryConfiguration config;
                if (existingConfig.isPresent()) {
                    config = existingConfig.get();
                    updateConfigurationFields(config, configData);
                } else {
                    config = new SalaryConfiguration(subadmin, employee);
                    updateConfigurationFields(config, configData);
                }
                if (!validateConfiguration(config))
                    continue;
                salaryConfigRepository.save(config);
                successCount++;
            }
            return ResponseEntity.ok(Map.of("success", true, "updated", successCount));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating salary configuration: " + e.getMessage());
        }
    }

    @Autowired
    private SalaryConfigurationRepository salaryConfigRepository;

    @Autowired
    private SubAdminRepo subadminRepository;

    @GetMapping("")
    public ResponseEntity<?> getSalaryConfiguration(@PathVariable Integer subadminId) {
        try {
            // Use the new method that handles multiple configurations properly
            Optional<SalaryConfiguration> config = salaryConfigRepository
                    .findFirstBySubadminIdOrderByIdDesc(subadminId);

            if (config.isPresent()) {
                return ResponseEntity.ok(config.get());
            } else {
                // Return default configuration if none exists
                SalaryConfiguration defaultConfig = new SalaryConfiguration();
                return ResponseEntity.ok(defaultConfig);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching salary configuration: " + e.getMessage());
        }
    }

    @PostMapping("")
    public ResponseEntity<?> createOrUpdateSalaryConfiguration(
            @PathVariable Integer subadminId,
            @RequestBody SalaryConfiguration configData) {

        try {
            // Validate subadmin exists
            Subadmin subadmin = subadminRepository.findById(subadminId).orElse(null);
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found");
            }

            // Check if configuration already exists (use the new method)
            Optional<SalaryConfiguration> existingConfig = salaryConfigRepository
                    .findFirstBySubadminIdOrderByIdDesc(subadminId);

            SalaryConfiguration config;
            if (existingConfig.isPresent()) {
                // Update existing configuration
                config = existingConfig.get();
                updateConfigurationFields(config, configData);
            } else {
                // Create new configuration
                config = new SalaryConfiguration(subadmin);
                updateConfigurationFields(config, configData);
            }

            // Validate percentages
            if (!validateConfiguration(config)) {
                return ResponseEntity.badRequest().body("Invalid configuration: Percentages must be between 0 and 100");
            }

            SalaryConfiguration savedConfig = salaryConfigRepository.save(config);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", existingConfig.isPresent() ? "Salary configuration updated successfully"
                    : "Salary configuration created successfully");
            response.put("configuration", savedConfig);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving salary configuration: " + e.getMessage());
        }
    }

    private void updateConfigurationFields(SalaryConfiguration config, SalaryConfiguration configData) {
        if (configData.getBasicPercentage() != null) {
            config.setBasicPercentage(configData.getBasicPercentage());
        }
        if (configData.getHraPercentage() != null) {
            config.setHraPercentage(configData.getHraPercentage());
        }
        if (configData.getDaPercentage() != null) {
            config.setDaPercentage(configData.getDaPercentage());
        }
        if (configData.getSpecialAllowancePercentage() != null) {
            config.setSpecialAllowancePercentage(configData.getSpecialAllowancePercentage());
        }
        if (configData.getProfessionalTax() != null) {
            config.setProfessionalTax(configData.getProfessionalTax());
        }
        if (configData.getTdsPercentage() != null) {
            config.setTdsPercentage(configData.getTdsPercentage());
        }
        if (configData.getTransportAllowance() != null) {
            config.setTransportAllowance(configData.getTransportAllowance());
        }
        if (configData.getMedicalAllowance() != null) {
            config.setMedicalAllowance(configData.getMedicalAllowance());
        }
        if (configData.getFoodAllowance() != null) {
            config.setFoodAllowance(configData.getFoodAllowance());
        }
        if (configData.getPfPercentage() != null) {
            config.setPfPercentage(configData.getPfPercentage());
        }
        if (configData.getEsiPercentage() != null) {
            config.setEsiPercentage(configData.getEsiPercentage());
        }
    }

    private boolean validateConfiguration(SalaryConfiguration config) {
        return config.getBasicPercentage() >= 0 && config.getBasicPercentage() <= 100 &&
                config.getHraPercentage() >= 0 && config.getHraPercentage() <= 100 &&
                config.getDaPercentage() >= 0 && config.getDaPercentage() <= 100 &&
                config.getSpecialAllowancePercentage() >= 0 && config.getSpecialAllowancePercentage() <= 100 &&
                config.getTdsPercentage() >= 0 && config.getTdsPercentage() <= 100 &&
                config.getPfPercentage() >= 0 && config.getPfPercentage() <= 100 &&
                config.getEsiPercentage() >= 0 && config.getEsiPercentage() <= 100;
    }
}
