package com.jaywant.demo.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;

@Service
public class EmployeePasswordResetService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmployeeService employeeService;

    // In-memory storage for OTPs (for demo; consider persistent or cache for prod)
    private final Map<String, String> otpStorage = new HashMap<>();

    /**
     * Generate OTP, store it, and email it to the employee’s address using the
     * Subadmin’s email.
     */
    public void sendResetOTP(String email) {
        Employee emp = employeeService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Subadmin subadmin = emp.getSubadmin();
        if (subadmin == null) {
            throw new RuntimeException("No Subadmin associated with this employee");
        }

        String otp = generateOTP();
        otpStorage.put(email, otp);

        String subject = "WTL Tourism – Password Reset OTP";
        String message = "<p>Hello,</p>"
                + "<p>Your password reset OTP is: <strong>" + otp + "</strong></p>"
                + "<p>This code is valid for 15 minutes.</p>"
                + "<p>If you didn’t request this, please ignore this email.</p>";

        // Use Subadmin's email by passing subadminId
        boolean sent = emailService.sendHtmlEmail(subadmin.getId(), message, subject, email);
        if (!sent) {
            throw new RuntimeException("Failed to send OTP email");
        }
    }

    /**
     * Verify user-supplied OTP.
     */
    public boolean verifyOTP(String email, String otp) {
        String stored = otpStorage.get(email);
        if (stored != null && stored.equals(otp)) {
            otpStorage.remove(email);
            return true;
        }
        return false;
    }

    /**
     * After OTP verification, update the employee’s password.
     */
    public void resetPassword(String email, String newPassword) {
        Employee emp = employeeService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        employeeService.updatePassword(emp.getEmpId(), newPassword);
    }

    private String generateOTP() {
        return UUID.randomUUID().toString().substring(0, 6);
    }
}