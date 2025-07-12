package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CloudPunchController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    /**
     * Direct punch endpoint for biometric devices
     * URL: POST https://yourdomain.com/api/punch
     * 
     * This endpoint receives data directly from biometric devices
     * and processes it into the HRM attendance system
     */
    @PostMapping("/punch")
    @Transactional
    public ResponseEntity<?> receivePunchData(@RequestBody Map<String, Object> punchData) {
        try {
            System.out.println("🌐 [CLOUD PUNCH] Received data from device: " + punchData);

            // Extract punch data
            String empCode = (String) punchData.get("emp_code");
            String deviceSerial = (String) punchData.get("device_serial");
            String punchTimeStr = (String) punchData.get("punch_time");
            String punchStateStr = (String) punchData.get("punch_state");
            String verifyType = (String) punchData.get("verify_type");

            // Validate required fields
            if (empCode == null || punchTimeStr == null || punchStateStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing required fields: emp_code, punch_time, punch_state"
                ));
            }

            // Parse punch data
            LocalDateTime punchDateTime = parseDateTime(punchTimeStr);
            Integer punchState = safeParseInteger(punchStateStr);

            if (punchDateTime == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid punch_time format: " + punchTimeStr
                ));
            }

            // Find employee
            Employee employee = findEmployee(empCode);
            if (employee == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Employee not found for emp_code: " + empCode
                ));
            }

            // Process attendance
            boolean success = processCloudPunch(employee, punchDateTime, punchState, deviceSerial, punchData);

            if (success) {
                String punchType = (punchState != null && punchState == 0) ? "IN" : "OUT";
                System.out.println("✅ [CLOUD PUNCH] " + employee.getFullName() + " - " + 
                                 punchDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                                 " - " + punchType + " [Device: " + deviceSerial + "]");

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Punch recorded successfully",
                    "employee", employee.getFullName(),
                    "punch_time", punchDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "punch_type", punchType,
                    "device_serial", deviceSerial
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to process punch data"
                ));
            }

        } catch (Exception e) {
            System.err.println("❌ [CLOUD PUNCH] Error processing punch: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * Bulk punch endpoint for multiple punches
     * URL: POST https://yourdomain.com/api/punch/bulk
     */
    @PostMapping("/punch/bulk")
    @Transactional
    public ResponseEntity<?> receiveBulkPunchData(@RequestBody Map<String, Object> bulkData) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> punches = (List<Map<String, Object>>) bulkData.get("punches");
            String deviceSerial = (String) bulkData.get("device_serial");

            if (punches == null || punches.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No punch data provided"
                ));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;

            for (Map<String, Object> punchData : punches) {
                try {
                    punchData.put("device_serial", deviceSerial); // Ensure device serial is set
                    ResponseEntity<?> result = receivePunchData(punchData);
                    
                    if (result.getStatusCode().is2xxSuccessful()) {
                        successCount++;
                        results.add(Map.of("success", true, "data", punchData));
                    } else {
                        results.add(Map.of("success", false, "data", punchData, "error", result.getBody()));
                    }
                } catch (Exception e) {
                    results.add(Map.of("success", false, "data", punchData, "error", e.getMessage()));
                }
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "total_punches", punches.size(),
                "successful_punches", successCount,
                "failed_punches", punches.size() - successCount,
                "results", results,
                "device_serial", deviceSerial
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Bulk processing error: " + e.getMessage()
            ));
        }
    }

    /**
     * Device heartbeat endpoint
     * URL: POST https://yourdomain.com/api/device/heartbeat
     */
    @PostMapping("/device/heartbeat")
    public ResponseEntity<?> deviceHeartbeat(@RequestBody Map<String, Object> deviceInfo) {
        try {
            String deviceSerial = (String) deviceInfo.get("device_serial");
            String deviceStatus = (String) deviceInfo.get("status");
            String deviceIp = (String) deviceInfo.get("device_ip");

            System.out.println("💓 [HEARTBEAT] Device: " + deviceSerial + " Status: " + deviceStatus + " IP: " + deviceIp);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Heartbeat received",
                "server_time", LocalDateTime.now(),
                "device_serial", deviceSerial
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Process cloud punch with "First Punch In Only" logic
     */
    @Transactional
    private boolean processCloudPunch(Employee employee, LocalDateTime punchDateTime, 
                                    Integer punchState, String deviceSerial, Map<String, Object> rawData) {
        try {
            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();
            String dateStr = punchDate.toString();

            // Find or create attendance record
            Optional<Attendance> existingOpt = attendanceRepo.findByEmployeeAndDate(employee, dateStr);
            
            Attendance attendance;
            if (existingOpt.isPresent()) {
                attendance = existingOpt.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Set biometric fields
            attendance.setBiometricUserId(String.valueOf(employee.getEmpId()));
            attendance.setDeviceSerial(deviceSerial);
            attendance.setVerifyType("fingerprint");
            attendance.setPunchSource("BIOMETRIC");
            attendance.setRawData(rawData.toString());

            // Apply "First Punch In Only" logic
            if (punchState != null && punchState == 0) {
                // Punch IN - Only set if no punch-in exists yet
                if (attendance.getPunchInTime() == null) {
                    attendance.setPunchInTime(punchTime);
                    attendance.setPunchIn(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    System.out.println("✅ First punch IN recorded (CLOUD): " + punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                } else {
                    System.out.println("⚠️ Duplicate punch IN ignored (CLOUD) - keeping first punch: " +
                            attendance.getPunchInTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    return true; // Don't save, but return success
                }
            } else {
                // Punch OUT - Always update to latest
                attendance.setPunchOutTime(punchTime);
                attendance.setPunchOut(punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                System.out.println("✅ Punch OUT recorded (CLOUD): " + punchTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }

            // Calculate working hours if both times are set
            if (attendance.getPunchInTime() != null && attendance.getPunchOutTime() != null) {
                attendance.calculateDurations();
                System.out.println("📊 Working hours calculated: " + attendance.getWorkingHours());
            }

            // Save attendance
            attendanceRepo.save(attendance);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error processing cloud punch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find employee by emp_code
     */
    private Employee findEmployee(String empCode) {
        try {
            Integer empId = Integer.parseInt(empCode);
            Optional<Employee> empOpt = employeeRepo.findById(empId);
            return empOpt.orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse datetime from string
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // Handle different formats
            if (dateTimeStr.contains("T")) {
                dateTimeStr = dateTimeStr.replace("T", " ");
            }
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safely parse integer
     */
    private Integer safeParseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
