package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EnhancedAttendanceService {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private SubAdminRepo subAdminRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Enhanced attendance creation that handles both manual and biometric
     * attendance
     */
    @Transactional
    public List<Attendance> addEnhancedAttendance(int subAdminId, String fullName, List<Attendance> attendances) {
        Employee emp = employeeRepo.findByFullName(fullName);
        Subadmin subadmin = subAdminRepo.findById(subAdminId).orElse(null);

        if (emp == null || subadmin == null) {
            throw new RuntimeException("Employee or Subadmin not found");
        }

        // Check if employee has biometric enrollment
        boolean hasBiometricEnrollment = hasEmployeeBiometricEnrollment(emp.getEmpId());

        // Process each attendance record
        for (Attendance attendance : attendances) {
            attendance.setEmployee(emp);

            // Set attendance source
            if (hasBiometricEnrollment && isWorkingFromOffice(attendance)) {
                attendance.setAttendanceSource("BIOMETRIC");
            } else {
                attendance.setAttendanceSource("MANUAL");
            }

            // Handle different attendance types
            processAttendanceByType(attendance, emp);
        }

        List<Attendance> savedAttendances = attendanceRepo.saveAll(attendances);

        // Broadcast updates via WebSocket
        for (Attendance attendance : savedAttendances) {
            broadcastAttendanceUpdate(subAdminId, attendance);
        }

        return savedAttendances;
    }

    /**
     * Add lunch in/out times (always manual)
     */
    @Transactional
    public Attendance addLunchAttendance(int subAdminId, int empId, String date,
            LocalTime lunchInTime, LocalTime lunchOutTime) {
        Employee emp = employeeRepo.findById(empId).orElse(null);
        if (emp == null) {
            throw new RuntimeException("Employee not found");
        }

        // Find existing attendance record for the date
        Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(emp, date);

        Attendance attendance;
        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
        } else {
            // Create new attendance record
            attendance = new Attendance();
            attendance.setEmployee(emp);
            attendance.setDate(date);
            attendance.setStatus("Present");
            attendance.setAttendanceSource("MANUAL");
        }

        // Set lunch times
        if (lunchInTime != null) {
            attendance.setLunchInTime(lunchInTime);
        }
        if (lunchOutTime != null) {
            attendance.setLunchOutTime(lunchOutTime);
        }

        // Recalculate durations
        attendance.calculateDurations();

        Attendance savedAttendance = attendanceRepo.save(attendance);

        // Broadcast update
        broadcastLunchUpdate(subAdminId, savedAttendance);

        return savedAttendance;
    }

    /**
     * Add work from field attendance (always manual)
     */
    @Transactional
    public Attendance addWorkFromFieldAttendance(int subAdminId, int empId, String date,
            LocalTime startTime, LocalTime endTime,
            String location, String workDescription) {
        Employee emp = employeeRepo.findById(empId).orElse(null);
        if (emp == null) {
            throw new RuntimeException("Employee not found");
        }

        // Find existing attendance record
        Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(emp, date);

        Attendance attendance;
        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
        } else {
            attendance = new Attendance();
            attendance.setEmployee(emp);
            attendance.setDate(date);
            attendance.setStatus("Present");
        }

        // Set work from field details
        attendance.setAttendanceType("WORK_FROM_FIELD");
        attendance.setAttendanceSource("MANUAL");
        attendance.setPunchInTime(startTime);
        attendance.setPunchOutTime(endTime);
        attendance.setFieldLocation(location);
        attendance.setWorkDescription(workDescription);

        // Calculate durations
        attendance.calculateDurations();

        Attendance savedAttendance = attendanceRepo.save(attendance);

        // Broadcast update
        broadcastWorkFromFieldUpdate(subAdminId, savedAttendance);

        return savedAttendance;
    }

    /**
     * Get attendance with source information
     */
    public List<Attendance> getAttendanceWithSource(int subAdminId, int empId) {
        Employee emp = employeeRepo.findBySubadminIdAndEmpId(subAdminId, empId);
        if (emp == null) {
            throw new RuntimeException("Employee not found");
        }

        List<Attendance> attendances = attendanceRepo.findByEmployee(emp);

        // Enhance with source information
        for (Attendance attendance : attendances) {
            if (attendance.getAttendanceSource() == null) {
                // Legacy data - determine source based on employee enrollment
                boolean hasBiometric = hasEmployeeBiometricEnrollment(empId);
                if (hasBiometric && isWorkingFromOffice(attendance)) {
                    attendance.setAttendanceSource("BIOMETRIC");
                } else {
                    attendance.setAttendanceSource("MANUAL");
                }
            }
        }

        return attendances;
    }

    /**
     * Check if employee has biometric enrollment
     */
    private boolean hasEmployeeBiometricEnrollment(Integer empId) {
        List<EmployeeDeviceMapping> mappings = mappingRepo.findByHrmEmployeeId(empId);
        return mappings.stream().anyMatch(mapping -> mapping.getFingerprintEnrolled() &&
                mapping.getEnrollmentStatus() == EmployeeDeviceMapping.EnrollmentStatus.COMPLETED);
    }

    /**
     * Determine if attendance is for office work (vs work from field)
     */
    private boolean isWorkingFromOffice(Attendance attendance) {
        return !"WORK_FROM_FIELD".equals(attendance.getAttendanceType());
    }

    /**
     * Process attendance based on type and source
     */
    private void processAttendanceByType(Attendance attendance, Employee employee) {
        String attendanceType = attendance.getAttendanceType();

        if ("WORK_FROM_FIELD".equals(attendanceType)) {
            // Work from field is always manual
            attendance.setAttendanceSource("MANUAL");
        } else if ("OFFICE".equals(attendanceType) || attendanceType == null) {
            // Office attendance - check if biometric is available
            boolean hasBiometric = hasEmployeeBiometricEnrollment(employee.getEmpId());
            if (hasBiometric) {
                attendance.setAttendanceSource("BIOMETRIC");
            } else {
                attendance.setAttendanceSource("MANUAL");
            }
        }

        // Calculate durations
        attendance.calculateDurations();
    }

    /**
     * Broadcast attendance update via WebSocket
     */
    private void broadcastAttendanceUpdate(Integer subadminId, Attendance attendance) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "ATTENDANCE_UPDATE");
        update.put("employeeId", attendance.getEmployee().getEmpId());
        update.put("employeeName", attendance.getEmployee().getFullName());
        update.put("date", attendance.getDate());
        update.put("punchInTime", attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : null);
        update.put("punchOutTime",
                attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : null);
        update.put("status", attendance.getStatus());
        update.put("source", attendance.getAttendanceSource());
        update.put("attendanceType", attendance.getAttendanceType());

        messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);
    }

    /**
     * Broadcast lunch update via WebSocket
     */
    private void broadcastLunchUpdate(Integer subadminId, Attendance attendance) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "LUNCH_UPDATE");
        update.put("employeeId", attendance.getEmployee().getEmpId());
        update.put("employeeName", attendance.getEmployee().getFullName());
        update.put("date", attendance.getDate());
        update.put("lunchInTime", attendance.getLunchInTime() != null ? attendance.getLunchInTime().toString() : null);
        update.put("lunchOutTime",
                attendance.getLunchOutTime() != null ? attendance.getLunchOutTime().toString() : null);

        messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);
    }

    /**
     * Broadcast work from field update via WebSocket
     */
    private void broadcastWorkFromFieldUpdate(Integer subadminId, Attendance attendance) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "WORK_FROM_FIELD_UPDATE");
        update.put("employeeId", attendance.getEmployee().getEmpId());
        update.put("employeeName", attendance.getEmployee().getFullName());
        update.put("date", attendance.getDate());
        update.put("startTime", attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : null);
        update.put("endTime", attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : null);
        update.put("location", attendance.getFieldLocation());
        update.put("workDescription", attendance.getWorkDescription());

        messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);
    }

    /**
     * Get attendance statistics with source breakdown
     */
    public Map<String, Object> getAttendanceStatistics(Integer subadminId, String fromDate, String toDate) {
        List<Attendance> attendances = attendanceRepo.findByDateRange(fromDate, toDate);

        // Filter by subadmin
        attendances = attendances.stream()
                .filter(att -> att.getEmployee().getSubadmin().getId() == subadminId)
                .toList();

        Map<String, Object> stats = new HashMap<>();

        long biometricCount = attendances.stream()
                .filter(att -> "BIOMETRIC".equals(att.getAttendanceSource()))
                .count();

        long manualCount = attendances.stream()
                .filter(att -> "MANUAL".equals(att.getAttendanceSource()))
                .count();

        long workFromFieldCount = attendances.stream()
                .filter(att -> "WORK_FROM_FIELD".equals(att.getAttendanceType()))
                .count();

        stats.put("totalAttendance", attendances.size());
        stats.put("biometricAttendance", biometricCount);
        stats.put("manualAttendance", manualCount);
        stats.put("workFromFieldAttendance", workFromFieldCount);
        stats.put("fromDate", fromDate);
        stats.put("toDate", toDate);

        return stats;
    }
}
