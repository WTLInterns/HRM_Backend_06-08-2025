package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.*;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.FirebaseService;
import com.jaywant.demo.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
@CrossOrigin(origins = "*")
public class FCMController {

    @Autowired
    private EmployeeRepo employeeRepository;

    @Autowired
    private SubAdminRepo subadminRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FirebaseService firebaseService;

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String title = request.get("title");
            String body = request.get("body");

            if (token == null || title == null || body == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing required fields: token, title, body");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Send notification using Firebase service
            String messageId = firebaseService.sendNotification(token, title, body, null);

            Map<String, Object> response = new HashMap<>();
            if (messageId != null && !messageId.equals("firebase-not-available") && !messageId.equals("no-token")) {
                response.put("success", true);
                response.put("message", "Notification sent successfully");
                response.put("messageId", messageId);
            } else {
                response.put("success", false);
                response.put("error", "Failed to send notification");
                response.put("messageId", messageId);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to send notification: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/register-token")
    public ResponseEntity<String> registerToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            System.out.println(token);
            String userType = request.get("userType"); // "EMPLOYEE" or "SUBADMIN"
            System.out.println("******************************");
            Integer userId = Integer.parseInt(request.get("userId"));

            if (token == null || userType == null || userId == null) {
                return ResponseEntity.badRequest().body("Missing required fields");
            }

            if ("EMPLOYEE".equals(userType)) {
                Employee employee = employeeRepository.findById(userId).orElse(null);
                if (employee != null) {
                    employee.setFcmToken(token);
                    employeeRepository.save(employee);
                }
            } else if ("SUBADMIN".equals(userType)) {
                Subadmin subadmin = subadminRepository.findById(userId).orElse(null);
                if (subadmin != null) {
                    subadmin.setFcmToken(token);
                    subadminRepository.save(subadmin);
                }
            }

            return ResponseEntity.ok("Token registered successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to register token: " + e.getMessage());
        }
    }

    @PutMapping("/update-preferences")
    public ResponseEntity<String> updateNotificationPreferences(@RequestBody Map<String, Object> request) {
        try {
            String userType = (String) request.get("userType");
            Integer userId = Integer.parseInt(request.get("userId").toString());
            Boolean enabled = (Boolean) request.get("notificationsEnabled");

            if ("EMPLOYEE".equals(userType)) {
                Employee employee = employeeRepository.findById(userId).orElse(null);
                if (employee != null) {
                    employee.setNotificationsEnabled(enabled);
                    employeeRepository.save(employee);
                }
            } else if ("SUBADMIN".equals(userType)) {
                Subadmin subadmin = subadminRepository.findById(userId).orElse(null);
                if (subadmin != null) {
                    subadmin.setNotificationsEnabled(enabled);
                    subadminRepository.save(subadmin);
                }
            }

            return ResponseEntity.ok("Preferences updated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update preferences: " + e.getMessage());
        }
    }

    @GetMapping("/notifications/{userType}/{userId}")
    public ResponseEntity<List<NotificationLog>> getNotifications(
            @PathVariable String userType, @PathVariable Integer userId) {
        try {
            List<NotificationLog> notifications = notificationService.getNotificationHistory(userType, userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/notifications/{userType}/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable String userType, @PathVariable Integer userId) {
        try {
            long count = notificationService.getUnreadCount(userType, userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(0L);
        }
    }

    @PutMapping("/notifications/{id}/mark-read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok("Marked as read");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to mark as read");
        }
    }

    @PutMapping("/notifications/{userType}/{userId}/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(
            @PathVariable String userType, @PathVariable Integer userId) {
        try {
            int updatedCount = notificationService.markAllAsRead(userType, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All notifications marked as read");
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to mark all as read");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
