package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Service.BiometricDataProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/biometric-management")
@CrossOrigin(origins = "*")
public class BiometricManagementController {

    @Autowired
    private BiometricDataProcessingService processingService;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    /**
     * Process biometric punch data manually
     */
    @PostMapping("/process-punch")
    public ResponseEntity<?> processPunch(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String timeStr = (String) request.get("timeStr");
            String status = (String) request.get("status");
            String deviceIp = (String) request.get("deviceIp");
            String verifyType = (String) request.get("verifyType");

            BiometricDataProcessingService.BiometricPunchData punchData = new BiometricDataProcessingService.BiometricPunchData(
                    userId, timeStr, status, deviceIp);

            if (verifyType != null) {
                punchData.setVerifyType(verifyType);
            }

            BiometricDataProcessingService.ProcessingResult result = processingService.processBiometricPunch(punchData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            if (result.getData() != null) {
                response.put("attendance", result.getData().getAttendance());
                response.put("punchType", result.getData().getPunchType());
                response.put("newRecord", result.getData().isNewRecord());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing punch: " + e.getMessage());
        }
    }

    /**
     * Get biometric attendance logs for an employee
     */
    @GetMapping("/attendance-logs/{empId}")
    public ResponseEntity<?> getAttendanceLogs(
            @PathVariable Integer empId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

            List<Attendance> attendanceList = attendanceRepo.findByEmployeeAndDateRange(
                    employee, start.toString(), end.toString());

            // Filter biometric attendance only
            List<Attendance> biometricAttendance = attendanceList.stream()
                    .filter(att -> "BIOMETRIC".equals(att.getAttendanceSource()))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("empId", empId);
            response.put("employeeName", employee.getFullName());
            response.put("startDate", start);
            response.put("endDate", end);
            response.put("totalRecords", biometricAttendance.size());
            response.put("attendanceLogs", biometricAttendance);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching attendance logs: " + e.getMessage());
        }
    }

    /**
     * Get biometric device statistics
     */
    @GetMapping("/device-stats/{terminalSerial}")
    public ResponseEntity<?> getDeviceStats(@PathVariable String terminalSerial) {
        try {
            List<EmployeeDeviceMapping> mappings = mappingRepo.findByTerminalSerial(terminalSerial);

            long enrolledEmployees = mappings.stream()
                    .filter(EmployeeDeviceMapping::getFingerprintEnrolled)
                    .count();

            // Get today's punches for this terminal
            String today = LocalDate.now().toString();
            List<Attendance> todayPunches = attendanceRepo.findByDeviceSerialAndDate(terminalSerial, today);

            Map<String, Object> stats = new HashMap<>();
            stats.put("terminalSerial", terminalSerial);
            stats.put("totalRegisteredEmployees", mappings.size());
            stats.put("enrolledEmployees", enrolledEmployees);
            stats.put("todayPunches", todayPunches.size());
            stats.put("lastActivity", getLastActivity(terminalSerial));

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching device stats: " + e.getMessage());
        }
    }

    /**
     * Bulk process biometric data from device export
     */
    @PostMapping("/bulk-process")
    public ResponseEntity<?> bulkProcessBiometricData(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> punchDataList = (List<Map<String, Object>>) request.get("punchData");
            String deviceIp = (String) request.get("deviceIp");

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            for (Map<String, Object> punchMap : punchDataList) {
                try {
                    String userId = (String) punchMap.get("userId");
                    String timeStr = (String) punchMap.get("timeStr");
                    String status = (String) punchMap.get("status");

                    BiometricDataProcessingService.BiometricPunchData punchData = new BiometricDataProcessingService.BiometricPunchData(
                            userId, timeStr, status, deviceIp);

                    BiometricDataProcessingService.ProcessingResult result = processingService
                            .processBiometricPunch(punchData);

                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("userId", userId);
                    resultMap.put("timeStr", timeStr);
                    resultMap.put("success", result.isSuccess());
                    resultMap.put("message", result.getMessage());

                    results.add(resultMap);

                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        errorCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("userId", punchMap.get("userId"));
                    errorResult.put("success", false);
                    errorResult.put("message", "Error: " + e.getMessage());
                    results.add(errorResult);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalProcessed", punchDataList.size());
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("results", results);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error bulk processing: " + e.getMessage());
        }
    }

    /**
     * Get employee biometric summary
     */
    @GetMapping("/employee-summary/{empId}")
    public ResponseEntity<?> getEmployeeBiometricSummary(@PathVariable Integer empId) {
        try {
            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                return ResponseEntity.badRequest().body("Employee not found");
            }

            List<EmployeeDeviceMapping> mappings = mappingRepo.findByHrmEmployeeId(empId);

            // Get recent biometric attendance
            String startDate = LocalDate.now().minusDays(30).toString();
            String endDate = LocalDate.now().toString();

            List<Attendance> recentAttendance = attendanceRepo.findByEmployeeAndDateRange(
                    employee, startDate, endDate).stream()
                    .filter(att -> "BIOMETRIC".equals(att.getAttendanceSource()))
                    .collect(Collectors.toList());

            Map<String, Object> summary = new HashMap<>();
            summary.put("empId", empId);
            summary.put("employeeName", employee.getFullName());
            summary.put("registeredDevices", mappings.size());
            summary.put("enrolledDevices",
                    mappings.stream().filter(EmployeeDeviceMapping::getFingerprintEnrolled).count());
            summary.put("recentBiometricPunches", recentAttendance.size());
            summary.put("lastBiometricPunch", getLastBiometricPunch(recentAttendance));
            summary.put("deviceMappings", mappings);

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching employee summary: " + e.getMessage());
        }
    }

    /**
     * Sync missing punches (handle edge cases)
     */
    @PostMapping("/sync-missing-punches")
    public ResponseEntity<?> syncMissingPunches(@RequestBody Map<String, Object> request) {
        try {
            String terminalSerial = (String) request.get("terminalSerial");
            String date = (String) request.get("date");

            // Find employees with missing check-out punches
            List<Attendance> incompleteAttendance = attendanceRepo.findIncompleteAttendanceByDate(date);

            List<Map<String, Object>> syncResults = new ArrayList<>();

            for (Attendance attendance : incompleteAttendance) {
                if (attendance.getPunchInTime() != null && attendance.getPunchOutTime() == null) {
                    // Auto-assign check-out time (e.g., 6 PM)
                    attendance.setPunchOutTime(java.time.LocalTime.of(18, 0));
                    attendance.setStatus("Present");
                    attendance.calculateDurations();
                    attendanceRepo.save(attendance);

                    Map<String, Object> syncResult = new HashMap<>();
                    syncResult.put("empId", attendance.getEmployee().getEmpId());
                    syncResult.put("employeeName", attendance.getEmployee().getFullName());
                    syncResult.put("action", "Auto check-out assigned");
                    syncResults.add(syncResult);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("date", date);
            response.put("syncedRecords", syncResults.size());
            response.put("syncResults", syncResults);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error syncing missing punches: " + e.getMessage());
        }
    }

    // Helper methods
    private String getLastActivity(String terminalSerial) {
        List<Attendance> recentActivity = attendanceRepo.findRecentActivityByTerminal(terminalSerial, 1);
        if (!recentActivity.isEmpty()) {
            Attendance last = recentActivity.get(0);
            return last.getDate() + " " +
                    (last.getPunchOutTime() != null ? last.getPunchOutTime() : last.getPunchInTime());
        }
        return "No recent activity";
    }

    private String getLastBiometricPunch(List<Attendance> attendanceList) {
        if (attendanceList.isEmpty()) {
            return "No biometric punches found";
        }

        Attendance latest = attendanceList.stream()
                .max(Comparator.comparing(att -> LocalDateTime.parse(att.getDate() + " " +
                        (att.getPunchOutTime() != null ? att.getPunchOutTime() : att.getPunchInTime()))))
                .orElse(null);

        if (latest != null) {
            return latest.getDate() + " " +
                    (latest.getPunchOutTime() != null ? latest.getPunchOutTime() : latest.getPunchInTime());
        }

        return "No recent punches";
    }
}
