package com.jaywant.demo.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jaywant.demo.Entity.Subadmin;

@Service
public class SubAdminPasswordResetService {

  @Autowired
  private EmailService emailService;

  @Autowired
  private SubAdminService subAdminService;

  // Temporary in-memory storage for OTPs (email -> OTP)
  private final Map<String, String> otpStorage = new HashMap<>();

  // Generate and send OTP to the provided email
  public void sendResetOTP(String email) {
    // This will now return a Subadmin or throw an exception if not found.
    Subadmin subAdmin = subAdminService.getSubAdminByEmail(email);
    String otp = generateOTP();
    otpStorage.put(email, otp); // Store OTP in memory

    // This calls the sendHtmlEmail method that uses Master Admin's email as the
    // sender
    boolean emailSent = emailService.sendHtmlEmail(otp, "Password Reset OTP", email);
    if (!emailSent) {
      throw new RuntimeException("Failed to send OTP email");
    }
  }

  // Generate a simple 6-character OTP
  private String generateOTP() {
    return UUID.randomUUID().toString().substring(0, 6);
  }

  // Verify the provided OTP against the stored OTP
  public boolean verifyOTP(String email, String otp) {
    String storedOtp = otpStorage.get(email);
    if (storedOtp != null && storedOtp.equals(otp)) {
      otpStorage.remove(email); // Clear OTP after successful validation
      return true;
    }
    return false;
  }

  // Reset the password for the subadmin identified by email.
  public void resetPassword(String email, String newPassword) {
    Subadmin subAdmin = subAdminService.getSubAdminByEmail(email);
    subAdminService.updatePassword(subAdmin.getId(), newPassword);
  }
}