package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class F22DataSyncService {

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sync F22 punch data every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void syncF22Data() {
        try {
            // Get all active terminals
            List<EmployeeDeviceMapping> mappings = mappingRepo.findAll();

            for (EmployeeDeviceMapping mapping : mappings) {
                if (mapping.getFingerprintEnrolled()) {
                    syncTerminalData(mapping);
                }
            }
        } catch (Exception e) {
            System.err.println("Error syncing F22 data: " + e.getMessage());
        }
    }

    private void syncTerminalData(EmployeeDeviceMapping mapping) {
        try {
            // This would call F22 API to get punch data
            // For now, we'll simulate checking for new punch data

            String terminalIp = "192.168.1.201"; // Your F22 IP
            String apiUrl = "http://" + terminalIp + ":4370/api/attendance/recent";

            // Note: This is a placeholder - actual F22 API endpoints may differ
            // You would need to check F22 documentation for correct endpoints

            System.out.println("Checking for new punch data for terminal: " + mapping.getTerminalSerial());

            // For demonstration, we'll create a manual sync method
            checkForManualSync(mapping);

        } catch (Exception e) {
            System.err.println("Error syncing terminal " + mapping.getTerminalSerial() + ": " + e.getMessage());
        }
    }

    /**
     * Manual method to create attendance records
     * This simulates what would happen when F22 sends punch data
     */
    private void checkForManualSync(EmployeeDeviceMapping mapping) {
        // This is where you would process actual F22 punch data
        // For now, it's a placeholder for the sync logic

        Employee employee = employeeRepo.findById(mapping.getHrmEmployeeId()).orElse(null);
        if (employee == null)
            return;

        String today = LocalDate.now().toString();

        // Check if attendance already exists for today
        Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(employee, today);

        if (existingAttendance.isEmpty()) {
            // This is where you would create attendance based on F22 data
            System.out.println("Ready to sync data for employee: " + employee.getFullName());
        }
    }

    /**
     * Manual method to sync specific punch data
     * Call this when you know there's new punch data
     */
    public void syncPunchData(Integer empId, String terminalSerial, String punchType, String punchTime) {
        try {
            EmployeeDeviceMapping mapping = mappingRepo
                    .findByHrmEmployeeIdAndTerminalSerial(empId, terminalSerial)
                    .orElse(null);

            if (mapping == null) {
                System.err.println("No mapping found for employee " + empId + " on terminal " + terminalSerial);
                return;
            }

            Employee employee = employeeRepo.findById(empId).orElse(null);
            if (employee == null) {
                System.err.println("Employee not found: " + empId);
                return;
            }

            String today = LocalDate.now().toString();

            // Find or create attendance record
            Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(employee, today);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(today);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Parse and set punch time
            LocalTime time = LocalTime.parse(punchTime, DateTimeFormatter.ofPattern("HH:mm"));

            if ("check_in".equals(punchType)) {
                attendance.setPunchInTime(time);
                System.out.println("Synced check-in for " + employee.getFullName() + " at " + punchTime);
            } else if ("check_out".equals(punchType)) {
                attendance.setPunchOutTime(time);
                System.out.println("Synced check-out for " + employee.getFullName() + " at " + punchTime);
            }

            // Calculate durations
            attendance.calculateDurations();

            // Save attendance
            attendanceRepo.save(attendance);

            System.out.println("Attendance synced successfully for employee: " + employee.getFullName());

        } catch (Exception e) {
            System.err.println("Error syncing punch data: " + e.getMessage());
        }
    }

    /**
     * Get punch data from F22 machine (placeholder)
     * This would connect to actual F22 API
     */
    public List<Map<String, Object>> getF22PunchData(String terminalSerial) {
        // This is a placeholder for actual F22 API integration
        // You would implement the actual F22 API calls here

        try {
            String apiUrl = "http://192.168.1.201:4370/api/attendance/today";

            // This would be the actual API call to F22
            // Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

            System.out.println("Would fetch punch data from F22 terminal: " + terminalSerial);

            // Return empty list for now
            return List.of();

        } catch (Exception e) {
            System.err.println("Error fetching F22 data: " + e.getMessage());
            return List.of();
        }
    }
}
