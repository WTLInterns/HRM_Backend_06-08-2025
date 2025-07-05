//package com.jaywant.demo.Service;
package com.jaywant.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.jaywant.demo.Entity.Subadmin;

@Service
public class SubAdminEmailService {

  @Autowired
  private JavaMailSender javaMailSender;

  // Sends the sub-admin's credentials via email using the actual password from
  // the backend.
  public void sendSubAdminCredentials(Subadmin subAdmin) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(subAdmin.getEmail());
    message.setSubject("🎉 Welcome to WTL Tourism Pvt. Ltd. - Your HRM SubAdmin Access");

    String fullName = capitalize(subAdmin.getName()) + " " + capitalize(subAdmin.getLastname());

    String body = "Hello " + fullName + ",\n\n"
        + "🎉 Welcome to WTL Tourism Pvt. Ltd.!\n\n"
        + "We're excited to have you on board as a HRM-Sub-Admin. Your account has been created successfully. Below are your login credentials:\n\n"
        + "📧 Email: " + subAdmin.getEmail() + "\n"
        + "🔐 Password: " + subAdmin.getPassword() + "\n\n"
        + "👉 Click here to log in: https://managifyhr.com\n\n"
        + "⚠️ Please change your password after your first login to ensure security.\n\n"
        + "If you face any issues, feel free to contact our support team.\n\n"
        + "Thanks & Regards,\n"
        + "Team WTL Tourism Pvt. Ltd.";

    message.setText(body);
    javaMailSender.send(message);
  }

  // In this notification method, we want to send the sub-admin's actual password
  // from the backend.
  // Therefore, we no longer override the password.
  public void sendSubAdminNotification(Subadmin subAdmin) {
    sendSubAdminCredentials(subAdmin);
  }

  // Utility method to capitalize a string properly.
  private String capitalize(String str) {
    if (str == null || str.isEmpty())
      return str;
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }
}
