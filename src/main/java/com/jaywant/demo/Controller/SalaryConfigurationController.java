package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.SalaryConfiguration;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.SalaryConfigurationRepository;
import com.jaywant.demo.Repo.SubAdminRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/salary-config")
@CrossOrigin(origins = "*")
public class SalaryConfigurationController {

    @Autowired
    private SalaryConfigurationRepository salaryConfigRepository;

    @Autowired
    private SubAdminRepo subadminRepository;

    @GetMapping("/{subadminId}")
    public ResponseEntity<?> getSalaryConfiguration(@PathVariable Integer subadminId) {
        try {
            Optional<SalaryConfiguration> config = salaryConfigRepository.findBySubadminId(subadminId);

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

    @PostMapping("/{subadminId}")
    public ResponseEntity<?> createOrUpdateSalaryConfiguration(
            @PathVariable Integer subadminId,
            @RequestBody SalaryConfiguration configData) {

        try {
            // Validate subadmin exists
            Subadmin subadmin = subadminRepository.findById(subadminId).orElse(null);
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found");
            }

            // Check if configuration already exists
            Optional<SalaryConfiguration> existingConfig = salaryConfigRepository.findBySubadminId(subadminId);

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
