package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.EmployeeDeviceMappingRepo;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ZKTecoTcpListenerService {

    @Autowired
    private EmployeeDeviceMappingRepo mappingRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRunning = false;
    private static final int LISTEN_PORT = 8000; // ZKTeco alternative port

    @PostConstruct
    public void startTcpListener() {
        try {
            executorService = Executors.newCachedThreadPool();
            startServer();
            System.out.println("ZKTeco TCP Listener started on port " + LISTEN_PORT);
        } catch (Exception e) {
            System.err.println("Failed to start ZKTeco TCP Listener: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopTcpListener() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            System.out.println("ZKTeco TCP Listener stopped");
        } catch (Exception e) {
            System.err.println("Error stopping TCP Listener: " + e.getMessage());
        }
    }

    private void startServer() {
        executorService.submit(() -> {
            try {
                serverSocket = new ServerSocket(LISTEN_PORT);
                isRunning = true;

                System.out.println("Listening for ZKTeco device connections on port " + LISTEN_PORT);

                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("ZKTeco device connected: " + clientSocket.getInetAddress());

                        // Handle each device connection in separate thread
                        executorService.submit(() -> handleDeviceConnection(clientSocket));

                    } catch (IOException e) {
                        if (isRunning) {
                            System.err.println("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error starting TCP server: " + e.getMessage());
            }
        });
    }

    private void handleDeviceConnection(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String deviceIp = clientSocket.getInetAddress().getHostAddress();
            System.out.println("Handling connection from device: " + deviceIp);

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received data from " + deviceIp + ": " + line);
                processDeviceData(line, deviceIp);
            }

        } catch (IOException e) {
            System.err.println("Error handling device connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void processDeviceData(String data, String deviceIp) {
        try {
            // Parse ZKTeco data format
            // Common format: "USERID\tTIME\tSTATUS\tVERIFY"
            // Example: "1\t2025-01-09 09:30:00\t1\t1"

            String[] parts = data.split("\t");
            if (parts.length >= 3) {
                String userId = parts[0].trim();
                String timeStr = parts[1].trim();
                String status = parts[2].trim(); // 1=in, 0=out

                processAttendanceData(userId, timeStr, status, deviceIp);
            } else {
                System.out.println("Unknown data format: " + data);
            }

        } catch (Exception e) {
            System.err.println("Error processing device data: " + e.getMessage());
        }
    }

    private void processAttendanceData(String machineUserId, String timeStr, String status, String deviceIp) {
        try {
            // Find employee mapping by machine user ID and device IP
            Integer easytimeEmployeeId = Integer.parseInt(machineUserId);

            // Find terminal by IP
            String terminalSerial = findTerminalByIp(deviceIp);
            if (terminalSerial == null) {
                System.err.println("No terminal found for IP: " + deviceIp);
                return;
            }

            // Find employee mapping
            Optional<EmployeeDeviceMapping> mappingOpt = mappingRepo
                    .findByEasytimeEmployeeIdAndTerminalSerial(easytimeEmployeeId, terminalSerial);

            if (mappingOpt.isEmpty()) {
                System.err.println("No employee mapping found for machine user " + machineUserId +
                        " on terminal " + terminalSerial);
                return;
            }

            EmployeeDeviceMapping mapping = mappingOpt.get();
            Employee employee = employeeRepo.findById(mapping.getHrmEmployeeId()).orElse(null);

            if (employee == null) {
                System.err.println("Employee not found: " + mapping.getHrmEmployeeId());
                return;
            }

            // Parse time
            LocalDateTime punchDateTime;
            try {
                punchDateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                // Try alternative format
                punchDateTime = LocalDateTime.now();
            }

            LocalDate punchDate = punchDateTime.toLocalDate();
            LocalTime punchTime = punchDateTime.toLocalTime();
            String dateStr = punchDate.toString();

            // Find or create attendance record
            Optional<Attendance> existingAttendance = attendanceRepo.findByEmployeeAndDate(employee, dateStr);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
            } else {
                attendance = new Attendance();
                attendance.setEmployee(employee);
                attendance.setDate(dateStr);
                attendance.setStatus("Present");
                attendance.setAttendanceSource("BIOMETRIC");
                attendance.setAttendanceType("OFFICE");
            }

            // Set enhanced biometric fields
            attendance.setPunchSource("BIOMETRIC");
            attendance.setBiometricUserId(machineUserId);
            attendance.setDeviceSerial(terminalSerial);
            attendance.setVerifyType("fingerprint");
            attendance.setRawData(machineUserId + "\t" + timeStr + "\t" + status + "\t" + deviceIp);

            // Set punch time based on status
            String punchType;
            if ("1".equals(status)) { // Check in
                attendance.setPunchInTime(punchTime);
                punchType = "check_in";
            } else { // Check out
                attendance.setPunchOutTime(punchTime);
                punchType = "check_out";
            }

            // Calculate durations
            attendance.calculateDurations();

            // Save attendance
            Attendance savedAttendance = attendanceRepo.save(attendance);

            // Send real-time notification
            broadcastAttendanceUpdate(employee.getSubadmin().getId(), savedAttendance, punchType);

            System.out.println("✅ Real-time attendance processed: " + employee.getFullName() +
                    " " + punchType + " at " + punchTime);

        } catch (Exception e) {
            System.err.println("Error processing attendance data: " + e.getMessage());
        }
    }

    private String findTerminalByIp(String deviceIp) {
        // For now, return the known terminal serial for your F22
        if ("192.168.1.201".equals(deviceIp)) {
            return "BOCK194960340";
        }
        return null;
    }

    private void broadcastAttendanceUpdate(Integer subadminId, Attendance attendance, String punchType) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "REAL_TIME_BIOMETRIC");
            update.put("employeeId", attendance.getEmployee().getEmpId());
            update.put("employeeName", attendance.getEmployee().getFullName());
            update.put("date", attendance.getDate());
            update.put("punchType", punchType);
            update.put("punchInTime",
                    attendance.getPunchInTime() != null ? attendance.getPunchInTime().toString() : null);
            update.put("punchOutTime",
                    attendance.getPunchOutTime() != null ? attendance.getPunchOutTime().toString() : null);
            update.put("source", "BIOMETRIC");
            update.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/attendance/" + subadminId, update);

            System.out.println("📡 Real-time update sent for " + attendance.getEmployee().getFullName());

        } catch (Exception e) {
            System.err.println("Error broadcasting update: " + e.getMessage());
        }
    }

    public boolean isListening() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    public Map<String, Object> getListenerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("listening", isListening());
        status.put("port", LISTEN_PORT);
        status.put("serverRunning", isRunning);
        return status;
    }

    /**
     * Process test punch data (for testing purposes)
     */
    public boolean processTestPunchData(String data, String deviceIp) {
        try {
            System.out.println("Processing test punch data: " + data + " from " + deviceIp);
            processDeviceData(data, deviceIp);
            return true;
        } catch (Exception e) {
            System.err.println("Error processing test punch data: " + e.getMessage());
            return false;
        }
    }
}
