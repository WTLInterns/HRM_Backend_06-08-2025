package com.jaywant.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jaywant.demo.Entity.Employee;

@Service
public class EmployeeEmailService {

  @Autowired
  private EmailService emailService;

  /**
   * Sends the employee their login credentials using the Subadmin’s email.
   * 
   * @param subadminId The ID of the Subadmin sending the email
   * @param employee   The employee receiving the credentials
   * @return true if email is sent successfully, false otherwise
   */
  public boolean sendEmployeeCredentials(int subadminId, Employee employee) {
    String to = employee.getEmail();
    String subject = "🎉 Your Employee Access";

    // Capitalize names
    String fullName = capitalize(employee.getFirstName()) + " "
        + capitalize(employee.getLastName());

    // Get company name from Subadmin, with fallback
    String companyName = (employee.getSubadmin() != null && employee.getSubadmin().getRegistercompanyname() != null)
        ? employee.getSubadmin().getRegistercompanyname()
        : "Your Company";

    // Build a simple HTML message
    StringBuilder sb = new StringBuilder();
    sb.append("<p>Hello ").append(fullName).append(",</p>");
    sb.append("<p>Your employee account has been created successfully. Below are your login details:</p>");
    sb.append("<ul>")
        .append("<li><strong>Email:</strong> ").append(employee.getEmail()).append("</li>")
        .append("<li><strong>Password:</strong> ").append(employee.getPassword()).append("</li>")
        .append("</ul>");
    sb.append("<p><a href=\"https://managifyhr.com\" target=\"_blank\">Click here to log in</a></p>");
    sb.append(
        "<p><strong>⚠️ Please change your password after your first login to keep your account secure.</strong></p>");
    sb.append("<p>If you have any issues, contact our support team.</p>");
    sb.append("<p>Thanks & Regards,<br>Team ").append(companyName).append("</p>");

    return emailService.sendHtmlEmail(subadminId, sb.toString(), subject, to);
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty())
      return str;
    return Character.toUpperCase(str.charAt(0))
        + str.substring(1).toLowerCase();
  }
}