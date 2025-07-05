package com.jaywant.demo.Service;

import com.jaywant.demo.Entity.*;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.NotificationLogRepository;
import com.jaywant.demo.Repo.SubAdminRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private EmployeeRepo employeeRepository;

    @Autowired
    private SubAdminRepo subadminRepository;

    public void sendLeaveApplicationNotification(int subadminId, Employee employee, LeaveForm leaveForm) {
        try {
            Subadmin subadmin = subadminRepository.findById(subadminId).orElse(null);
            if (subadmin == null) {
                return;
            }

            // Allow notifications if notificationsEnabled is null (default) or true
            if (Boolean.FALSE.equals(subadmin.getNotificationsEnabled())) {
                System.out.println("⚠️ Notifications disabled for subadmin: " + subadmin.getName());
                return;
            }

            String title = "New Leave Request";
            String body = String.format("📅 %s requested leave for %s - %s",
                    employee.getFullName(), leaveForm.getFromDate(), leaveForm.getToDate());

            Map<String, String> data = new HashMap<>();
            data.put("type", "LEAVE_APPLIED");
            data.put("leaveId", String.valueOf(leaveForm.getLeaveId()));
            data.put("employeeId", String.valueOf(employee.getEmpId()));
            data.put("employeeName", employee.getFullName());

            // Send Firebase notification
            String fcmMessageId = null;
            if (firebaseService.isValidToken(subadmin.getFcmToken())) {
                fcmMessageId = firebaseService.sendNotification(
                        subadmin.getFcmToken(), title, body, data);
            }

            // Save to notification log
            NotificationLog log = new NotificationLog(
                    "SUBADMIN", subadminId, title, body, "LEAVE_APPLIED", leaveForm.getLeaveId());
            log.setFcmMessageId(fcmMessageId);
            notificationLogRepository.save(log);

        } catch (Exception e) {
            System.err.println("Failed to send leave application notification: " + e.getMessage());
        }
    }

    public void sendLeaveApplicationNotification(int subadminId, Employee employee, LeaveForm leaveForm,
            String usertoken, String subadmintoken) {
        try {
            String title = "New Leave Request";
            String body = String.format("\uD83D\uDCC5 %s requested leave for %s - %s",
                    employee.getFullName(), leaveForm.getFromDate(), leaveForm.getToDate());

            Map<String, String> data = new HashMap<>();
            data.put("type", "LEAVE_APPLIED");
            data.put("leaveId", String.valueOf(leaveForm.getLeaveId()));
            data.put("employeeId", String.valueOf(employee.getEmpId()));
            data.put("employeeName", employee.getFullName());

            // Send Firebase notification to subadmin using subadmintoken
            String fcmMessageId = null;
            if (firebaseService.isValidToken(subadmintoken)) {
                fcmMessageId = firebaseService.sendNotification(
                        subadmintoken, title, body, data);
            }

            // Save to notification log
            NotificationLog log = new NotificationLog(
                    "SUBADMIN", subadminId, title, body, "LEAVE_APPLIED", leaveForm.getLeaveId());
            log.setFcmMessageId(fcmMessageId);
            notificationLogRepository.save(log);

        } catch (Exception e) {
            System.err.println("Failed to send leave application notification: " + e.getMessage());
        }
    }

    public void sendLeaveStatusNotification(int employeeId, LeaveForm leaveForm, String status) {
        try {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                return;
            }

            // Allow notifications if notificationsEnabled is null (default) or true
            if (Boolean.FALSE.equals(employee.getNotificationsEnabled())) {
                System.out.println("⚠️ Notifications disabled for employee: " + employee.getFullName());
                return;
            }

            String title = status.equals("Approved") ? "Leave Request Approved ✅" : "Leave Request Update ❌";
            String body = String.format("Your leave for %s - %s has been %s",
                    leaveForm.getFromDate(), leaveForm.getToDate(), status.toLowerCase());

            Map<String, String> data = new HashMap<>();
            data.put("type", "LEAVE_" + status.toUpperCase());
            data.put("leaveId", String.valueOf(leaveForm.getLeaveId()));
            data.put("status", status);

            // Send Firebase notification
            String fcmMessageId = null;
            if (firebaseService.isValidToken(employee.getFcmToken())) {
                fcmMessageId = firebaseService.sendNotification(
                        employee.getFcmToken(), title, body, data);
            }

            // Save to notification log
            NotificationLog log = new NotificationLog(
                    "EMPLOYEE", employeeId, title, body, "LEAVE_" + status.toUpperCase(), leaveForm.getLeaveId());
            log.setFcmMessageId(fcmMessageId);
            notificationLogRepository.save(log);

        } catch (Exception e) {
            System.err.println("Failed to send leave status notification: " + e.getMessage());
        }
    }

    public void sendLeaveStatusNotification(int employeeId, LeaveForm leaveForm, String status, String usertoken,
            String subadmintoken) {
        try {
            String title = status.equals("Approved") ? "Leave Request Approved ✅" : "Leave Request Update ❌";
            String body = String.format("Your leave for %s - %s has been %s",
                    leaveForm.getFromDate(), leaveForm.getToDate(), status.toLowerCase());

            Map<String, String> data = new HashMap<>();
            data.put("type", "LEAVE_" + status.toUpperCase());
            data.put("leaveId", String.valueOf(leaveForm.getLeaveId()));
            data.put("status", status);

            // Send Firebase notification to employee using usertoken
            String fcmMessageId = null;
            if (firebaseService.isValidToken(usertoken)) {
                fcmMessageId = firebaseService.sendNotification(
                        usertoken, title, body, data);
            }

            // Save to notification log
            NotificationLog log = new NotificationLog(
                    "EMPLOYEE", employeeId, title, body, "LEAVE_" + status.toUpperCase(), leaveForm.getLeaveId());
            log.setFcmMessageId(fcmMessageId);
            notificationLogRepository.save(log);

        } catch (Exception e) {
            System.err.println("Failed to send leave status notification: " + e.getMessage());
        }
    }

    public List<NotificationLog> getNotificationHistory(String userType, Integer userId) {
        if ("SUBADMIN".equals(userType)) {
            // For subadmins, exclude JOB_OPENING notifications (they shouldn't see their
            // own job postings)
            return notificationLogRepository.findByRecipientTypeAndRecipientIdAndNotificationTypeNotOrderBySentAtDesc(
                    userType, userId, "JOB_OPENING");
        } else {
            // For employees, show all notifications
            return notificationLogRepository.findByRecipientTypeAndRecipientIdOrderBySentAtDesc(userType, userId);
        }
    }

    public long getUnreadCount(String userType, Integer userId) {
        if ("SUBADMIN".equals(userType)) {
            // For subadmins, exclude JOB_OPENING notifications from count
            return notificationLogRepository.countUnreadNotificationsExcludingType(userType, userId, "JOB_OPENING");
        } else {
            // For employees, count all notifications
            return notificationLogRepository.countUnreadNotifications(userType, userId);
        }
    }

    public void markAsRead(Long notificationId) {
        notificationLogRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationLogRepository.save(notification);
        });
    }

    public int markAllAsRead(String userType, Integer userId) {
        if ("SUBADMIN".equals(userType)) {
            // For subadmins, exclude JOB_OPENING notifications (they shouldn't mark their
            // own job postings as read)
            return notificationLogRepository.markAllAsReadForSubadminExcludingType(userType, userId, "JOB_OPENING");
        } else {
            // For employees, mark all notifications as read
            return notificationLogRepository.markAllAsReadForUser(userType, userId);
        }
    }

    // Send job opening notification to all employees of a subadmin
    public void sendJobOpeningNotification(int subadminId, Opening opening, String subadminToken) {
        try {
            System.out.println("🔄 Starting job opening notification for subadmin ID: " + subadminId);
            System.out.println("🎯 Job opening details:");
            System.out.println("   - Role: " + opening.getRole());
            System.out.println("   - Location: " + opening.getLocation());
            System.out.println("   - Site Mode: " + opening.getSiteMode());
            System.out.println("   - Positions: " + opening.getPositions());
            System.out.println("   - Experience: " + opening.getExprience());
            System.out.println("   - Description: " + opening.getDescription());
            System.out.println("   - Work Type: " + opening.getWorkType());

            // Get all employees under this subadmin
            List<Employee> employees = employeeRepository.findBySubadminId(subadminId);
            System.out.println("👥 Found " + employees.size() + " employees under subadmin ID: " + subadminId);

            // Validate opening data
            if (opening.getRole() == null || opening.getLocation() == null) {
                System.err.println("❌ Invalid opening data - role or location is null");
                return;
            }

            String title = "🎯 New Job Opening Available!";
            String body = String.format("New position: %s at %s. Apply now!",
                    opening.getRole(), opening.getLocation());

            Map<String, String> data = new HashMap<>();
            data.put("type", "JOB_OPENING");
            data.put("openingId", String.valueOf(opening.getId()));
            data.put("role", opening.getRole());
            data.put("location", opening.getLocation());

            System.out.println("📧 Notification content:");
            System.out.println("   Title: " + title);
            System.out.println("   Body: " + body);

            int notificationsSent = 0;
            // Send notification to all employees
            for (Employee employee : employees) {
                // Check if notifications are enabled for this employee (allow null as enabled)
                if (Boolean.FALSE.equals(employee.getNotificationsEnabled())) {
                    System.out.println("⚠️ Notifications disabled for employee: " + employee.getFullName());
                    continue;
                }

                if (employee.getFcmToken() != null && !employee.getFcmToken().isEmpty()) {
                    System.out.println("📱 Sending notification to: " + employee.getFullName() + " (ID: "
                            + employee.getEmpId() + ")");
                    String fcmMessageId = firebaseService.sendNotification(
                            employee.getFcmToken(), title, body, data);
                    System.out.println("   FCM Message ID: " + fcmMessageId);

                    // Save notification log for each employee
                    NotificationLog log = new NotificationLog(
                            "EMPLOYEE", employee.getEmpId(), title, body, "JOB_OPENING", opening.getId());
                    log.setFcmMessageId(fcmMessageId);
                    notificationLogRepository.save(log);
                    notificationsSent++;
                } else {
                    System.out.println("⚠️ No FCM token for employee: " + employee.getFullName());
                }
            }

            System.out.println("✅ Job opening notification sent to " + notificationsSent + " out of " + employees.size()
                    + " employees");
        } catch (Exception e) {
            System.err.println("❌ Failed to send job opening notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Send resume submission notification to subadmin
    public void sendResumeSubmissionNotification(int employeeId, Resume resume, String userToken,
            String subadminToken) {
        try {
            System.out.println("🔄 Starting resume submission notification for employee ID: " + employeeId);

            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                System.err.println("❌ Employee not found with ID: " + employeeId);
                return;
            }

            Subadmin subadmin = employee.getSubadmin();
            if (subadmin == null) {
                System.err.println("❌ Subadmin not found for employee: " + employee.getFullName());
                return;
            }

            System.out.println("✅ Found subadmin: " + subadmin.getName() + " (ID: " + subadmin.getId() + ")");
            System.out.println("🔍 Subadmin FCM token: "
                    + (subadmin.getFcmToken() != null ? subadmin.getFcmToken().substring(0, 20) + "..." : "null"));
            System.out.println("🔍 Notifications enabled: " + subadmin.getNotificationsEnabled());

            // Allow notifications if notificationsEnabled is null (default) or true
            if (Boolean.FALSE.equals(subadmin.getNotificationsEnabled())) {
                System.out.println("⚠️ Notifications disabled for subadmin: " + subadmin.getName());
                return;
            }

            String title = "📄 New Resume Submitted";
            String body = String.format("%s submitted a resume for %s position",
                    employee.getFullName(), resume.getJobRole());

            Map<String, String> data = new HashMap<>();
            data.put("type", "RESUME_SUBMITTED");
            data.put("resumeId", String.valueOf(resume.getResumeId()));
            data.put("employeeId", String.valueOf(employeeId));
            data.put("employeeName", employee.getFullName());
            data.put("jobRole", resume.getJobRole());

            // Send Firebase notification to subadmin
            System.out.println("🔄 Preparing to send FCM notification...");
            System.out.println("📧 Title: " + title);
            System.out.println("📧 Body: " + body);

            String fcmMessageId = null;
            // Use the passed subadminToken first, fallback to stored token
            String tokenToUse = subadminToken;
            if (!firebaseService.isValidToken(tokenToUse)) {
                System.out.println("⚠️ Passed subadmin token invalid, using stored token");
                tokenToUse = subadmin.getFcmToken();
            }

            if (firebaseService.isValidToken(tokenToUse)) {
                System.out.println("✅ FCM token is valid, sending notification...");
                try {
                    fcmMessageId = firebaseService.sendNotification(tokenToUse, title, body, data);
                    System.out.println("📱 FCM message ID: " + fcmMessageId);
                } catch (Exception e) {
                    System.err.println(
                            "⚠️ FCM notification failed, but continuing with database notification: " + e.getMessage());
                    fcmMessageId = "fcm-failed-" + System.currentTimeMillis();
                }
            } else {
                System.err.println("❌ No valid FCM token available for subadmin: " + subadmin.getName());
                fcmMessageId = "no-token-" + System.currentTimeMillis();
            }

            // Save to notification log
            NotificationLog log = new NotificationLog(
                    "SUBADMIN", subadmin.getId(), title, body, "RESUME_SUBMITTED", resume.getResumeId());
            log.setFcmMessageId(fcmMessageId);
            notificationLogRepository.save(log);
            System.out.println("💾 Notification log saved with ID: " + log.getId());
            System.out.println("📋 Notification details - Type: RESUME_SUBMITTED, SubadminId: " + subadmin.getId()
                    + ", Title: " + title);

            System.out
                    .println("✅ Resume submission notification process completed for subadmin: " + subadmin.getName());
        } catch (Exception e) {
            System.err.println("❌ Failed to send resume submission notification: " + e.getMessage());
        }
    }
}