package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Service.EnhancedAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enhanced-attendance")
@CrossOrigin(origins = "*")
public class EnhancedAttendanceController {

    @Autowired
    private EnhancedAttendanceService enhancedAttendanceService;

    /**
     * Add lunch in/out times (Manual API - always hits our system)
     */
    @PostMapping("/{subadminId}/{empId}/lunch")
    public ResponseEntity<?> addLunchAttendance(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @RequestBody Map<String, String> lunchData) {
        try {
            String date = lunchData.get("date");
            String lunchInTimeStr = lunchData.get("lunchInTime");
            String lunchOutTimeStr = lunchData.get("lunchOutTime");

            LocalTime lunchInTime = null;
            LocalTime lunchOutTime = null;

            if (lunchInTimeStr != null && !lunchInTimeStr.isEmpty()) {
                lunchInTime = LocalTime.parse(lunchInTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }
            if (lunchOutTimeStr != null && !lunchOutTimeStr.isEmpty()) {
                lunchOutTime = LocalTime.parse(lunchOutTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            }

            Attendance attendance = enhancedAttendanceService.addLunchAttendance(
                subadminId, empId, date, lunchInTime, lunchOutTime
            );

            return ResponseEntity.ok(attendance);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error adding lunch attendance: " + e.getMessage());
        }
    }

    /**
     * Add work from field attendance (Manual API - always hits our system)
     */
    @PostMapping("/{subadminId}/{empId}/work-from-field")
    public ResponseEntity<?> addWorkFromFieldAttendance(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @RequestBody Map<String, String> fieldWorkData) {
        try {
            String date = fieldWorkData.get("date");
            String startTimeStr = fieldWorkData.get("startTime");
            String endTimeStr = fieldWorkData.get("endTime");
            String location = fieldWorkData.get("location");
            String workDescription = fieldWorkData.get("workDescription");

            LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

            Attendance attendance = enhancedAttendanceService.addWorkFromFieldAttendance(
                subadminId, empId, date, startTime, endTime, location, workDescription
            );

            return ResponseEntity.ok(attendance);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error adding work from field attendance: " + e.getMessage());
        }
    }

    /**
     * Get attendance with source information (biometric vs manual)
     */
    @GetMapping("/{subadminId}/{empId}/attendance-with-source")
    public ResponseEntity<?> getAttendanceWithSource(
            @PathVariable int subadminId,
            @PathVariable int empId) {
        try {
            List<Attendance> attendances = enhancedAttendanceService.getAttendanceWithSource(subadminId, empId);
            return ResponseEntity.ok(attendances);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching attendance: " + e.getMessage());
        }
    }

    /**
     * Get attendance statistics with source breakdown
     */
    @GetMapping("/{subadminId}/statistics")
    public ResponseEntity<?> getAttendanceStatistics(
            @PathVariable Integer subadminId,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        try {
            Map<String, Object> statistics = enhancedAttendanceService.getAttendanceStatistics(
                subadminId, fromDate, toDate
            );
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching statistics: " + e.getMessage());
        }
    }

    /**
     * Enhanced bulk attendance add with source detection
     */
    @PostMapping("/{subadminId}/bulk-add")
    public ResponseEntity<?> addBulkEnhancedAttendance(
            @PathVariable int subadminId,
            @RequestBody Map<String, Object> requestData) {
        try {
            String fullName = (String) requestData.get("fullName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceDataList = (List<Map<String, Object>>) requestData.get("attendances");

            // Convert to Attendance objects
            List<Attendance> attendances = attendanceDataList.stream()
                .map(this::mapToAttendance)
                .toList();

            List<Attendance> savedAttendances = enhancedAttendanceService.addEnhancedAttendance(
                subadminId, fullName, attendances
            );

            return ResponseEntity.ok(savedAttendances);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error adding bulk attendance: " + e.getMessage());
        }
    }

    /**
     * Quick punch in/out for mobile app (determines source automatically)
     */
    @PostMapping("/{subadminId}/{empId}/quick-punch")
    public ResponseEntity<?> quickPunch(
            @PathVariable int subadminId,
            @PathVariable int empId,
            @RequestBody Map<String, String> punchData) {
        try {
            String date = punchData.get("date");
            String punchType = punchData.get("punchType"); // "in" or "out"
            String timeStr = punchData.get("time");
            String attendanceType = punchData.getOrDefault("attendanceType", "OFFICE");

            LocalTime punchTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));

            // Create attendance object
            Attendance attendance = new Attendance();
            attendance.setDate(date);
            attendance.setAttendanceType(attendanceType);
            attendance.setStatus("Present");

            if ("in".equals(punchType)) {
                attendance.setPunchInTime(punchTime);
            } else if ("out".equals(punchType)) {
                attendance.setPunchOutTime(punchTime);
            }

            // Use enhanced service to determine source automatically
            List<Attendance> result = enhancedAttendanceService.addEnhancedAttendance(
                subadminId, null, List.of(attendance)
            );

            return ResponseEntity.ok(result.get(0));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing quick punch: " + e.getMessage());
        }
    }

    /**
     * Get today's attendance summary for dashboard
     */
    @GetMapping("/{subadminId}/today-summary")
    public ResponseEntity<?> getTodayAttendanceSummary(@PathVariable Integer subadminId) {
        try {
            String today = java.time.LocalDate.now().toString();
            Map<String, Object> summary = enhancedAttendanceService.getAttendanceStatistics(
                subadminId, today, today
            );
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching today's summary: " + e.getMessage());
        }
    }

    /**
     * Helper method to convert map to Attendance object
     */
    private Attendance mapToAttendance(Map<String, Object> data) {
        Attendance attendance = new Attendance();
        attendance.setDate((String) data.get("date"));
        attendance.setStatus((String) data.getOrDefault("status", "Present"));
        attendance.setAttendanceType((String) data.getOrDefault("attendanceType", "OFFICE"));

        if (data.get("punchInTime") != null) {
            attendance.setPunchInTime(LocalTime.parse((String) data.get("punchInTime")));
        }
        if (data.get("punchOutTime") != null) {
            attendance.setPunchOutTime(LocalTime.parse((String) data.get("punchOutTime")));
        }
        if (data.get("lunchInTime") != null) {
            attendance.setLunchInTime(LocalTime.parse((String) data.get("lunchInTime")));
        }
        if (data.get("lunchOutTime") != null) {
            attendance.setLunchOutTime(LocalTime.parse((String) data.get("lunchOutTime")));
        }
        if (data.get("reason") != null) {
            attendance.setReason((String) data.get("reason"));
        }
        if (data.get("fieldLocation") != null) {
            attendance.setFieldLocation((String) data.get("fieldLocation"));
        }
        if (data.get("workDescription") != null) {
            attendance.setWorkDescription((String) data.get("workDescription"));
        }

        return attendance;
    }
}
