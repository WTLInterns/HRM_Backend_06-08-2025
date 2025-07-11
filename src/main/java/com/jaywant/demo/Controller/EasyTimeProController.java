package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.SubadminTerminal;
import com.jaywant.demo.Repo.SubadminTerminalRepo;
import com.jaywant.demo.Service.EasyTimeProIntegrationService;
import com.jaywant.demo.Service.EasyTimeProApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/easytime")
@CrossOrigin(origins = "*")
public class EasyTimeProController {

    @Autowired
    private EasyTimeProIntegrationService integrationService;

    @Autowired
    private EasyTimeProApiService apiService;

    @Autowired
    private SubadminTerminalRepo terminalRepo;

    /**
     * Register a new terminal for a subadmin
     */
    @PostMapping("/terminals/{subadminId}/register")
    public ResponseEntity<?> registerTerminal(
            @PathVariable Integer subadminId,
            @RequestBody Map<String, String> terminalData) {
        try {
            SubadminTerminal terminal = new SubadminTerminal();
            terminal.setSubadminId(subadminId);
            terminal.setTerminalSerial(terminalData.get("terminalSerial"));
            terminal.setTerminalName(terminalData.get("terminalName"));
            terminal.setLocation(terminalData.get("location"));
            terminal.setEasytimeApiUrl(terminalData.get("easytimeApiUrl"));
            terminal.setApiToken(terminalData.get("apiToken"));
            terminal.setStatus(SubadminTerminal.TerminalStatus.ACTIVE);

            // Test connection before saving
            boolean connectionTest = apiService.testConnection(
                    terminal.getEasytimeApiUrl(),
                    terminal.getApiToken());

            if (!connectionTest) {
                return ResponseEntity.badRequest()
                        .body("Failed to connect to EasyTimePro API. Please check URL and token.");
            }

            SubadminTerminal savedTerminal = terminalRepo.save(terminal);
            return ResponseEntity.ok(savedTerminal);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering terminal: " + e.getMessage());
        }
    }

    /**
     * Get all terminals for a subadmin
     */
    @GetMapping("/terminals/{subadminId}")
    public ResponseEntity<?> getTerminals(@PathVariable Integer subadminId) {
        try {
            List<SubadminTerminal> terminals = terminalRepo.findBySubadminId(subadminId);
            return ResponseEntity.ok(terminals);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching terminals: " + e.getMessage());
        }
    }

    /**
     * Register employee on a specific terminal
     */
    @PostMapping("/terminals/{terminalSerial}/employees/{employeeId}/register")
    public ResponseEntity<?> registerEmployeeOnTerminal(
            @PathVariable String terminalSerial,
            @PathVariable Integer employeeId) {
        try {
            EmployeeDeviceMapping mapping = integrationService.registerEmployeeOnTerminal(
                    employeeId, terminalSerial);
            return ResponseEntity.ok(mapping);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error registering employee: " + e.getMessage());
        }
    }

    /**
     * Remove employee from terminal
     */
    @DeleteMapping("/terminals/{terminalSerial}/employees/{employeeId}")
    public ResponseEntity<?> removeEmployeeFromTerminal(
            @PathVariable String terminalSerial,
            @PathVariable Integer employeeId) {
        try {
            boolean success = integrationService.removeEmployeeFromTerminal(employeeId, terminalSerial);
            if (success) {
                return ResponseEntity.ok("Employee removed from terminal successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to remove employee from terminal");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error removing employee: " + e.getMessage());
        }
    }

    /**
     * Get employee mappings for a subadmin
     */
    @GetMapping("/mappings/{subadminId}")
    public ResponseEntity<?> getEmployeeMappings(@PathVariable Integer subadminId) {
        try {
            List<EmployeeDeviceMapping> mappings = integrationService.getEmployeeMappings(subadminId);
            return ResponseEntity.ok(mappings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching mappings: " + e.getMessage());
        }
    }

    /**
     * Webhook endpoint to receive attendance data from EasyTimePro
     */
    @PostMapping("/webhook/attendance")
    public ResponseEntity<?> receiveAttendanceWebhook(@RequestBody List<Map<String, Object>> transactions) {
        try {
            for (Map<String, Object> transaction : transactions) {
                integrationService.processAttendanceTransaction(transaction);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("processed", transactions.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing attendance webhook: " + e.getMessage());
        }
    }

    /**
     * Manual sync attendance data for a terminal
     */
    @PostMapping("/terminals/{terminalSerial}/sync")
    public ResponseEntity<?> syncAttendanceData(
            @PathVariable String terminalSerial,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            // Default to today if no dates provided
            if (fromDate == null) {
                fromDate = LocalDateTime.now().toLocalDate().toString();
            }
            if (toDate == null) {
                toDate = LocalDateTime.now().toLocalDate().toString();
            }

            integrationService.syncAttendanceData(terminalSerial, fromDate, toDate);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Attendance data synced successfully");
            response.put("fromDate", fromDate);
            response.put("toDate", toDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error syncing attendance data: " + e.getMessage());
        }
    }

    /**
     * Test terminal connection
     */
    @PostMapping("/terminals/{terminalSerial}/test")
    public ResponseEntity<?> testTerminalConnection(@PathVariable String terminalSerial) {
        try {
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found"));

            boolean connectionTest = apiService.testConnection(
                    terminal.getEasytimeApiUrl(),
                    terminal.getApiToken());

            Map<String, Object> response = new HashMap<>();
            response.put("terminalSerial", terminalSerial);
            response.put("connected", connectionTest);
            response.put("timestamp", LocalDateTime.now());

            if (connectionTest) {
                terminal.setStatus(SubadminTerminal.TerminalStatus.ACTIVE);
                terminal.setLastSyncAt(LocalDateTime.now());
                terminalRepo.save(terminal);
            } else {
                terminal.setStatus(SubadminTerminal.TerminalStatus.ERROR);
                terminalRepo.save(terminal);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error testing connection: " + e.getMessage());
        }
    }

    /**
     * Update terminal configuration
     */
    @PutMapping("/terminals/{terminalSerial}")
    public ResponseEntity<?> updateTerminal(
            @PathVariable String terminalSerial,
            @RequestBody Map<String, String> updateData) {
        try {
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found"));

            if (updateData.containsKey("terminalName")) {
                terminal.setTerminalName(updateData.get("terminalName"));
            }
            if (updateData.containsKey("location")) {
                terminal.setLocation(updateData.get("location"));
            }
            if (updateData.containsKey("easytimeApiUrl")) {
                terminal.setEasytimeApiUrl(updateData.get("easytimeApiUrl"));
            }
            if (updateData.containsKey("apiToken")) {
                terminal.setApiToken(updateData.get("apiToken"));
            }

            SubadminTerminal updatedTerminal = terminalRepo.save(terminal);
            return ResponseEntity.ok(updatedTerminal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating terminal: " + e.getMessage());
        }
    }

    /**
     * Get terminal status and statistics
     */
    @GetMapping("/terminals/{terminalSerial}/status")
    public ResponseEntity<?> getTerminalStatus(@PathVariable String terminalSerial) {
        try {
            SubadminTerminal terminal = terminalRepo.findByTerminalSerial(terminalSerial)
                    .orElseThrow(() -> new RuntimeException("Terminal not found"));

            Map<String, Object> status = new HashMap<>();
            status.put("terminal", terminal);
            status.put("enrolledEmployees", integrationService.getEmployeeMappings(terminal.getSubadminId()).size());
            status.put("lastSync", terminal.getLastSyncAt());
            status.put("status", terminal.getStatus());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching terminal status: " + e.getMessage());
        }
    }
}
