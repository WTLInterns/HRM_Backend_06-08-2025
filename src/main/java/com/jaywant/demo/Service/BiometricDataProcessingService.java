package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BiometricDataProcessingService {

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    /**
     * Process biometric punch data with advanced logic
     */
    @Transactional
    public ProcessingResult processBiometricPunch(BiometricPunchData punchData) {
        try {
            // 1. Validate and find employee mapping
            EmployeeMapping mapping = findEmployeeMapping(punchData);
            if (mapping == null) {
                return ProcessingResult.error("Employee mapping not found for device user: " + punchData.getUserId());
            }

            // 2. Parse and validate punch time
            LocalDateTime punchDateTime = parsePunchTime(punchData.getTimeStr());
            if (punchDateTime == null) {
                return ProcessingResult.error("Invalid punch time format: " + punchData.getTimeStr());
            }

            // 3. Handle duplicate punch detection
            if (isDuplicatePunch(mapping.employee, punchDateTime, punchData)) {
                return ProcessingResult.warning("Duplicate punch detected - ignored");
            }

            // 4. Process attendance with smart logic
            AttendanceResult result = processAttendanceWithSmartLogic(mapping, punchDateTime, punchData);

            return ProcessingResult.success(result);

        } catch (Exception e) {
            return ProcessingResult.error("Error processing biometric punch: " + e.getMessage());
        }
    }

    /**
     * Find employee mapping by device user ID and terminal
     */
    private EmployeeMapping findEmployeeMapping(BiometricPunchData punchData) {
        try {
            Integer easytimeEmployeeId = Integer.parseInt(punchData.getUserId());
            String terminalSerial = findTerminalByIp(punchData.getDeviceIp());

            if (terminalSerial == null) {
                return null;
            }

            Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo
                    .findByEasytimeEmployeeIdAndTerminalSerial(easytimeEmployeeId, terminalSerial);

            if (mappingOpt.isEmpty()) {
                return null;
            }

            EmployeeDeviceMapping mapping = mappingOpt.get();
            Employee employee = employeeRepo.findById(mapping.getHrmEmployeeId()).orElse(null);

            if (employee == null) {
                return null;
            }

            return new EmployeeMapping(employee, mapping, terminalSerial);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse punch time with multiple format support
     */
    private LocalDateTime parsePunchTime(String timeStr) {
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(timeStr, formatter);
            } catch (Exception e) {
                // Try next format
            }
        }

        // If all formats fail, use current time
        return LocalDateTime.now();
    }

    /**
     * Detect duplicate punches within time window
     */
    private boolean isDuplicatePunch(Employee employee, LocalDateTime punchTime, BiometricPunchData punchData) {
        LocalDateTime startWindow = punchTime.minusMinutes(2);
        LocalDateTime endWindow = punchTime.plusMinutes(2);

        // Check for recent punches within 2-minute window
        List<Attendance> recentAttendance = attendanceRepo.findByEmployeeAndDateRange(
                employee,
                startWindow.toLocalDate().toString(),
                startWindow.toLocalTime(),
                endWindow.toLocalTime());

        return recentAttendance.stream()
                .anyMatch(att -> Objects.equals(att.getBiometricUserId(), punchData.getUserId()) &&
                        Objects.equals(att.getDeviceSerial(), findTerminalByIp(punchData.getDeviceIp())));
    }

    /**
     * Process attendance with smart check-in/check-out logic
     */
    private AttendanceResult processAttendanceWithSmartLogic(EmployeeMapping mapping,
            LocalDateTime punchDateTime,
            BiometricPunchData punchData) {

        LocalDate punchDate = punchDateTime.toLocalDate();
        LocalTime punchTime = punchDateTime.toLocalTime();
        String dateStr = punchDate.toString();

        // Find or create attendance record
        Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(mapping.employee, dateStr);

        Attendance attendance;
        boolean isNewRecord = false;

        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
        } else {
            attendance = createNewAttendanceRecord(mapping.employee, dateStr);
            isNewRecord = true;
        }

        // Smart punch logic
        String punchType = determinePunchType(attendance, punchTime, punchData.getStatus());

        // Set biometric fields
        setBiometricFields(attendance, punchData, mapping.terminalSerial);

        // Apply punch based on type
        applyPunchTime(attendance, punchTime, punchType);

        // Calculate durations
        attendance.calculateDurations();

        // Save attendance
        Attendance savedAttendance = attendanceRepo.save(attendance);

        return new AttendanceResult(savedAttendance, punchType, isNewRecord);
    }

    /**
     * Determine punch type based on existing data and time
     */
    private String determinePunchType(Attendance attendance, LocalTime punchTime, String status) {
        // If status is provided by device, use it
        if ("1".equals(status)) {
            return "check_in";
        } else if ("0".equals(status)) {
            return "check_out";
        }

        // Smart logic based on existing punches
        if (attendance.getPunchInTime() == null) {
            return "check_in";
        } else if (attendance.getPunchOutTime() == null) {
            return "check_out";
        } else {
            // Both punches exist - determine based on time
            if (punchTime.isBefore(attendance.getPunchInTime())) {
                return "check_in"; // Earlier time, update check-in
            } else {
                return "check_out"; // Later time, update check-out
            }
        }
    }

    /**
     * Create new attendance record with defaults
     */
    private Attendance createNewAttendanceRecord(Employee employee, String dateStr) {
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(dateStr);
        attendance.setStatus("Present");
        attendance.setAttendanceSource("BIOMETRIC");
        attendance.setAttendanceType("OFFICE");
        return attendance;
    }

    /**
     * Set biometric-specific fields
     */
    private void setBiometricFields(Attendance attendance, BiometricPunchData punchData, String terminalSerial) {
        attendance.setPunchSource("BIOMETRIC");
        attendance.setBiometricUserId(punchData.getUserId());
        attendance.setDeviceSerial(terminalSerial);
        attendance.setVerifyType(punchData.getVerifyType() != null ? punchData.getVerifyType() : "fingerprint");
        attendance.setRawData(punchData.getRawData());
    }

    /**
     * Apply punch time based on type
     */
    private void applyPunchTime(Attendance attendance, LocalTime punchTime, String punchType) {
        if ("check_in".equals(punchType)) {
            attendance.setPunchInTime(punchTime);
        } else if ("check_out".equals(punchType)) {
            attendance.setPunchOutTime(punchTime);
        }
    }

    /**
     * Find terminal serial by IP address
     */
    private String findTerminalByIp(String deviceIp) {
        // This should match your existing logic
        if ("192.168.1.201".equals(deviceIp)) {
            return "BOCK194960340";
        }
        return null;
    }

    // Data classes
    public static class BiometricPunchData {
        private String userId;
        private String timeStr;
        private String status;
        private String deviceIp;
        private String verifyType;
        private String rawData;

        // Constructors, getters, setters
        public BiometricPunchData(String userId, String timeStr, String status, String deviceIp) {
            this.userId = userId;
            this.timeStr = timeStr;
            this.status = status;
            this.deviceIp = deviceIp;
            this.rawData = userId + "\t" + timeStr + "\t" + status + "\t" + deviceIp;
        }

        // Getters and setters
        public String getUserId() {
            return userId;
        }

        public String getTimeStr() {
            return timeStr;
        }

        public String getStatus() {
            return status;
        }

        public String getDeviceIp() {
            return deviceIp;
        }

        public String getVerifyType() {
            return verifyType;
        }

        public String getRawData() {
            return rawData;
        }

        public void setVerifyType(String verifyType) {
            this.verifyType = verifyType;
        }
    }

    private static class EmployeeMapping {
        Employee employee;
        EmployeeDeviceMapping mapping;
        String terminalSerial;

        EmployeeMapping(Employee employee, EmployeeDeviceMapping mapping, String terminalSerial) {
            this.employee = employee;
            this.mapping = mapping;
            this.terminalSerial = terminalSerial;
        }
    }

    public static class ProcessingResult {
        private boolean success;
        private String message;
        private AttendanceResult data;

        private ProcessingResult(boolean success, String message, AttendanceResult data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static ProcessingResult success(AttendanceResult data) {
            return new ProcessingResult(true, "Success", data);
        }

        public static ProcessingResult error(String message) {
            return new ProcessingResult(false, message, null);
        }

        public static ProcessingResult warning(String message) {
            return new ProcessingResult(true, message, null);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public AttendanceResult getData() {
            return data;
        }
    }

    public static class AttendanceResult {
        private Attendance attendance;
        private String punchType;
        private boolean newRecord;

        public AttendanceResult(Attendance attendance, String punchType, boolean newRecord) {
            this.attendance = attendance;
            this.punchType = punchType;
            this.newRecord = newRecord;
        }

        // Getters
        public Attendance getAttendance() {
            return attendance;
        }

        public String getPunchType() {
            return punchType;
        }

        public boolean isNewRecord() {
            return newRecord;
        }
    }
}
