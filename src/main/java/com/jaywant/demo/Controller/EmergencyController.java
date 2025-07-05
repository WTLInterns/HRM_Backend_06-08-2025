package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.NotificationLog;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.NotificationLogRepository;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
public class EmergencyController {

    @Autowired
    private EmployeeRepo employeeRepository;

    @Autowired
    private SubAdminRepo subadminRepository;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @PostMapping("/{subadminId}/send/{subadmintoken}")
    public ResponseEntity<?> sendEmergencyMessage(
            @PathVariable Integer subadminId,
            @PathVariable("subadmintoken") String subadminToken,
            @RequestBody Map<String, String> request) {

        try {
            String message = request.get("message");

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Emergency message cannot be empty");
            }

            // Get subadmin details
            Subadmin subadmin = subadminRepository.findById(subadminId).orElse(null);
            if (subadmin == null) {
                return ResponseEntity.badRequest().body("Subadmin not found");
            }

            // Get all employees under this subadmin
            List<Employee> employees = employeeRepository.findBySubadminId(subadminId);

            if (employees.isEmpty()) {
                return ResponseEntity.badRequest().body("No employees found under this subadmin");
            }

            String title = "🚨 EMERGENCY ALERT - " + subadmin.getRegistercompanyname();
            String body = message.trim();

            // Prepare notification data
            Map<String, String> data = new HashMap<>();
            data.put("type", "EMERGENCY_MESSAGE");
            data.put("subadminId", String.valueOf(subadminId));
            data.put("companyName", subadmin.getRegistercompanyname());
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            System.out.println("🚨 EMERGENCY MESSAGE ALERT 🚨");
            System.out.println("📧 From: " + subadmin.getRegistercompanyname() + " (Subadmin ID: " + subadminId + ")");
            System.out.println("📧 Title: " + title);
            System.out.println("📧 Message: " + body);
            System.out.println("👥 Target: " + employees.size() + " employees");

            int notificationsSent = 0;
            int notificationsLogged = 0;

            // Send notification to all employees
            for (Employee employee : employees) {
                try {
                    // Check if notifications are enabled for this employee (allow null as enabled)
                    if (Boolean.FALSE.equals(employee.getNotificationsEnabled())) {
                        System.out.println("⚠️ Notifications disabled for employee: " + employee.getFullName());
                        continue;
                    }

                    // Always log the emergency message regardless of FCM token availability
                    NotificationLog log = new NotificationLog(
                            "EMPLOYEE",
                            employee.getEmpId(),
                            title,
                            body,
                            "EMERGENCY_MESSAGE",
                            null // No specific entity ID for emergency messages
                    );

                    String fcmMessageId = null;

                    // Try to send FCM notification if token is available
                    if (employee.getFcmToken() != null && !employee.getFcmToken().isEmpty()) {
                        System.out.println("📱 Sending emergency notification to: " + employee.getFullName() + " (ID: "
                                + employee.getEmpId() + ")");
                        try {
                            fcmMessageId = firebaseService.sendNotification(
                                    employee.getFcmToken(), title, body, data);
                            System.out.println("   ✅ FCM Message ID: " + fcmMessageId);
                            notificationsSent++;
                        } catch (Exception fcmError) {
                            System.err.println(
                                    "   ❌ FCM failed for " + employee.getFullName() + ": " + fcmError.getMessage());
                            fcmMessageId = "fcm-failed-" + System.currentTimeMillis();
                        }
                    } else {
                        System.out
                                .println("⚠️ No FCM token for employee: " + employee.getFullName() + " - logging only");
                        fcmMessageId = "no-token-" + System.currentTimeMillis();
                    }

                    log.setFcmMessageId(fcmMessageId);
                    notificationLogRepository.save(log);
                    notificationsLogged++;

                } catch (Exception e) {
                    System.err.println("❌ Error processing employee " + employee.getFullName() + ": " + e.getMessage());
                }
            }

            System.out.println("📊 EMERGENCY MESSAGE SUMMARY:");
            System.out.println("   📱 FCM notifications sent: " + notificationsSent);
            System.out.println("   📝 Database logs created: " + notificationsLogged);
            System.out.println("   👥 Total employees: " + employees.size());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Emergency message processed successfully");
            response.put("employeeCount", employees.size());
            response.put("notificationsSent", notificationsSent);
            response.put("notificationsLogged", notificationsLogged);
            response.put("companyName", subadmin.getRegistercompanyname());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error sending emergency message: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to send emergency message: " + e.getMessage());
        }
    }
}
