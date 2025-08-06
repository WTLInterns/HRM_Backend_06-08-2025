package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.*;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.FirebaseService;
import com.jaywant.demo.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
@CrossOrigin(originPatterns = { "http://localhost:*", "https://admin.managifyhr.com", "https://*.managifyhr.com",
        "*" }, methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE,
                RequestMethod.OPTIONS }, allowedHeaders = "*", allowCredentials = "false")
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

    @GetMapping("/check-token/{employeeId}")
    public ResponseEntity<Map<String, Object>> checkEmployeeToken(@PathVariable Integer employeeId) {
        try {
            System.out.println("?? Checking FCM token for employee ID: " + employeeId);

            Employee employee = employeeRepository.findById(employeeId).orElse(null);

            Map<String, Object> response = new HashMap<>();

            if (employee == null) {
                response.put("success", false);
                response.put("message", "Employee not found with ID: " + employeeId);
                return ResponseEntity.notFound().build();
            }

            response.put("success", true);
            response.put("employeeId", employee.getEmpId());
            response.put("employeeName", employee.getFullName());
            response.put("email", employee.getEmail());

            if (employee.getFcmToken() != null && !employee.getFcmToken().isEmpty()) {
                response.put("hasToken", true);
                response.put("tokenLength", employee.getFcmToken().length());
                response.put("tokenPreview",
                        employee.getFcmToken().substring(0, Math.min(30, employee.getFcmToken().length())) + "...");
                response.put("fullToken", employee.getFcmToken());
                response.put("tokenUpdatedAt", employee.getFcmTokenUpdatedAt());
                response.put("notificationsEnabled", employee.getNotificationsEnabled());

                System.out.println("? Employee: " + employee.getFullName());
                System.out.println("   Token Length: " + employee.getFcmToken().length());
                System.out.println("   Token Preview: "
                        + employee.getFcmToken().substring(0, Math.min(30, employee.getFcmToken().length())) + "...");
                System.out.println("   Full Token: " + employee.getFcmToken());
                System.out.println("   Updated At: " + employee.getFcmTokenUpdatedAt());
            } else {
                response.put("hasToken", false);
                response.put("message", "No FCM token found for this employee");
                System.out.println("? No FCM token for employee: " + employee.getFullName());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("? Error checking token: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/clear-invalid-tokens/{subadminId}")
    public ResponseEntity<Map<String, Object>> clearInvalidTokens(@PathVariable Integer subadminId) {
        try {
            System.out.println("?? Clearing invalid FCM tokens for subadmin ID: " + subadminId);

            List<Employee> employees = employeeRepository.findBySubadminId(subadminId);
            int clearedCount = 0;

            for (Employee employee : employees) {
                if (employee.getFcmToken() != null && !employee.getFcmToken().isEmpty()) {
                    // Test the token by trying to send a test message
                    Map<String, String> testData = new HashMap<>();
                    testData.put("type", "TOKEN_TEST");

                    String result = firebaseService.sendNotification(employee.getFcmToken(), "Test", "Token validation",
                            testData);

                    // Check if notification failed (null result means failed)
                    if (result == null) {
                        System.out.println("? Invalid token for: " + employee.getFullName() + " - notification failed");
                        employee.setFcmToken(null);
                        employee.setFcmTokenUpdatedAt(null);
                        employeeRepository.save(employee);
                        clearedCount++;
                    } else {
                        System.out.println("? Valid token for: " + employee.getFullName());
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Invalid tokens cleared successfully");
            response.put("clearedCount", clearedCount);
            response.put("totalEmployees", employees.size());

            System.out.println(
                    "?? Cleared " + clearedCount + " invalid tokens out of " + employees.size() + " employees");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("? Error clearing invalid tokens: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test endpoint specifically for iOS notifications with detailed debugging
     */
    @PostMapping("/test-ios")
    public ResponseEntity<Map<String, Object>> testIOSNotification(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String title = request.getOrDefault("title", "iOS Test");
        String body = request.getOrDefault("body", "Testing iOS notification from Flutter app");

        System.out.println("🍎 =================================");
        System.out.println("🍎 TESTING iOS NOTIFICATION");
        System.out.println("🍎 =================================");
        System.out.println("📱 Token: " + token);
        System.out.println("📱 Token Length: " + (token != null ? token.length() : 0));
        System.out.println("📧 Title: " + title);
        System.out.println("📧 Body: " + body);

        Map<String, Object> response = new HashMap<>();

        if (token == null || token.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Check Firebase availability
            System.out.println("🔥 Firebase available: " + firebaseService.isFirebaseAvailable());

            // Check token validity
            System.out.println("✅ Token valid: " + firebaseService.isValidToken(token));

            // Modern Flutter FCM tokens can look like Android tokens even on iOS
            // So let's not rely on token format detection for Flutter apps
            System.out.println("📱 Token format analysis:");
            System.out.println("   - Contains ':' : " + token.contains(":"));
            System.out.println("   - Contains 'APA91' : " + token.contains("APA91"));
            System.out.println("   - Length: " + token.length());
            System.out.println("🍎 Note: Flutter FCM tokens can have Android-like format even on iOS");

            // Add iOS-specific data for testing
            Map<String, String> data = new HashMap<>();
            data.put("type", "IOS_TEST");
            data.put("platform", "iOS");
            data.put("source", "flutter_app");

            // Send notification using Firebase service
            System.out.println("🚀 Sending notification...");
            String messageId = firebaseService.sendNotification(token, title, body, data);
            System.out.println("📬 Message ID received: " + messageId);

            if (messageId != null && !messageId.equals("firebase-not-available") && !messageId.equals("no-token")) {
                response.put("success", true);
                response.put("message", "iOS notification sent successfully");
                response.put("messageId", messageId);
                response.put("tokenAnalysis", Map.of(
                        "length", token.length(),
                        "containsColon", token.contains(":"),
                        "containsAPA91", token.contains("APA91"),
                        "note", "Flutter FCM tokens can have Android-like format on iOS"));
                System.out.println("✅ iOS notification sent successfully!");
                System.out.println("🍎 Check your iOS device for the notification!");
            } else {
                response.put("success", false);
                response.put("error", "Failed to send iOS notification");
                response.put("messageId", messageId);
                response.put("possibleCauses", List.of(
                        "Firebase not properly initialized",
                        "Invalid token",
                        "APNs certificate not configured in Firebase",
                        "iOS app not properly configured for push notifications"));
                System.err.println("❌ iOS notification failed!");
                System.err.println("💡 Possible causes:");
                System.err.println("   1. Firebase not properly initialized");
                System.err.println("   2. APNs certificate not configured in Firebase Console");
                System.err.println("   3. iOS app not properly configured for push notifications");
            }

        } catch (Exception e) {
            System.err.println("❌ iOS notification exception: " + e.getMessage());
            System.err.println("🔍 Full stack trace:");
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Exception: " + e.getMessage());
            response.put("stackTrace", e.getStackTrace().toString());
        }

        System.out.println("🍎 =================================");
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to detect iOS tokens
     */
    private boolean isIOSToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // iOS tokens are typically 64 characters long and contain only hexadecimal
        // characters
        if (token.length() == 64 && token.matches("^[0-9a-fA-F]+$")) {
            return true;
        }

        // Additional check: Android tokens typically contain colons, underscores, or
        // hyphens
        if (!token.contains(":") && !token.contains("_") && !token.contains("-") && token.length() >= 60) {
            return true;
        }

        return false;
    }

    /**
     * Simple endpoint to analyze token type without sending notification
     */
    @PostMapping("/analyze-token")
    public ResponseEntity<Map<String, Object>> analyzeToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        Map<String, Object> response = new HashMap<>();

        if (token == null || token.trim().isEmpty()) {
            response.put("error", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }

        // Analyze token characteristics
        boolean containsColon = token.contains(":");
        boolean containsAPA91 = token.contains("APA91");
        boolean containsUnderscore = token.contains("_");
        boolean containsHyphen = token.contains("-");
        int tokenLength = token.length();

        // Determine token type based on characteristics
        String tokenType = "UNKNOWN";
        String confidence = "LOW";

        if (containsAPA91 && containsColon && tokenLength > 140) {
            tokenType = "ANDROID";
            confidence = "HIGH";
        } else if (tokenLength == 64 && token.matches("^[0-9a-fA-F]+$")) {
            tokenType = "iOS_LEGACY";
            confidence = "HIGH";
        } else if (!containsColon && !containsUnderscore && tokenLength >= 60 && tokenLength <= 70) {
            tokenType = "iOS_POSSIBLE";
            confidence = "MEDIUM";
        } else if (containsColon && tokenLength > 100) {
            tokenType = "ANDROID_OR_FLUTTER_iOS";
            confidence = "MEDIUM";
        }

        response.put("token", token);
        response.put("tokenLength", tokenLength);
        response.put("analysis", Map.of(
                "containsColon", containsColon,
                "containsAPA91", containsAPA91,
                "containsUnderscore", containsUnderscore,
                "containsHyphen", containsHyphen,
                "isHexOnly", token.matches("^[0-9a-fA-F]+$"),
                "tokenType", tokenType,
                "confidence", confidence));

        // Additional insights
        List<String> insights = new ArrayList<>();
        if (containsAPA91) {
            insights.add("Contains 'APA91' prefix - typical of Android FCM tokens");
        }
        if (containsColon) {
            insights.add("Contains ':' character - common in Android tokens");
        }
        if (tokenLength > 140) {
            insights.add("Very long token (>140 chars) - typical of Android");
        }
        if (tokenLength == 64 && token.matches("^[0-9a-fA-F]+$")) {
            insights.add("64-character hex string - classic iOS APNs token format");
        }
        if (tokenType.equals("ANDROID_OR_FLUTTER_iOS")) {
            insights.add("Modern Flutter apps can generate Android-style tokens even on iOS devices");
        }

        response.put("insights", insights);
        response.put("recommendation",
                tokenType.equals("ANDROID") ? "This appears to be an Android token. Test with Android device."
                        : tokenType.contains("iOS")
                                ? "This might be an iOS token. Verify the source device and ensure APNs is configured."
                                : "Token type unclear. Check the source device and app configuration.");

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a specific notification by ID
     */
    @DeleteMapping("/notifications/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🗑️ Attempting to delete notification with ID: " + notificationId);

            // Check if notification exists
            boolean exists = notificationService.notificationExists(notificationId);
            if (!exists) {
                System.err.println("❌ Notification not found with ID: " + notificationId);
                response.put("success", false);
                response.put("error", "Notification not found");
                return ResponseEntity.notFound().build();
            }

            // Delete the notification
            boolean deleted = notificationService.deleteNotification(notificationId);

            if (deleted) {
                System.out.println("✅ Notification deleted successfully with ID: " + notificationId);
                response.put("success", true);
                response.put("message", "Notification deleted successfully");
                response.put("deletedId", notificationId);
            } else {
                System.err.println("❌ Failed to delete notification with ID: " + notificationId);
                response.put("success", false);
                response.put("error", "Failed to delete notification");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            System.err.println("❌ Exception while deleting notification: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete multiple notifications by IDs
     */
    @DeleteMapping("/notifications/batch")
    public ResponseEntity<Map<String, Object>> deleteNotifications(@RequestBody Map<String, List<Long>> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Long> notificationIds = request.get("notificationIds");

            if (notificationIds == null || notificationIds.isEmpty()) {
                response.put("success", false);
                response.put("error", "No notification IDs provided");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("🗑️ Attempting to delete " + notificationIds.size() + " notifications");

            int deletedCount = notificationService.deleteNotifications(notificationIds);

            System.out.println(
                    "✅ Successfully deleted " + deletedCount + " out of " + notificationIds.size() + " notifications");

            response.put("success", true);
            response.put("message", "Notifications deleted successfully");
            response.put("deletedCount", deletedCount);
            response.put("requestedCount", notificationIds.size());

        } catch (Exception e) {
            System.err.println("❌ Exception while deleting notifications: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify CORS is working
     */
    @GetMapping("/cors-test")
    public ResponseEntity<Map<String, Object>> corsTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "CORS is working correctly!");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .body(response);
    }

    /**
     * Handle preflight OPTIONS requests for CORS
     */
    @RequestMapping(method = RequestMethod.OPTIONS, value = "/**")
    public ResponseEntity<Void> handlePreflight() {
        return ResponseEntity.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }
}