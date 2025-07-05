package com.jaywant.demo.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Entity.MasterAdmin;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Repo.MasterAdminRepo;

@Service
public class EmailService {

  private final String host = "smtp.gmail.com";

  @Autowired
  private SubAdminRepo subadminRepository;

  @Autowired
  private MasterAdminRepo masterAdminRepo; // Added to fetch Master Admin

  // Get mail session properties
  private Properties getMailProperties() {
    Properties properties = System.getProperties();
    properties.put("mail.smtp.host", host);
    properties.put("mail.smtp.port", 465);
    properties.put("mail.smtp.ssl.enable", true);
    properties.put("mail.smtp.auth", true);
    return properties;
  }

  // Get authenticated session for Subadmin
  private Session getSessionForSubadmin(Subadmin subadmin) {
    return Session.getInstance(getMailProperties(), new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(subadmin.getEmail(), subadmin.getEmailServerPassword());
      }
    });
  }

  // Get authenticated session for Master Admin
  private Session getSessionForMasterAdmin() {
    MasterAdmin masterAdmin = masterAdminRepo.findFirstByRoll("MASTER_ADMIN");
    if (masterAdmin == null) {
      throw new IllegalStateException("Master Admin not found");
    }
    return Session.getInstance(getMailProperties(), new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(masterAdmin.getEmail(), masterAdmin.getMasterAdminEmailServerPassword());
      }
    });
  }

  // Original method: Send HTML email using Subadmin's credentials
  public boolean sendHtmlEmail(int subadminId, String message, String subject, String to) {
    boolean f = false;
    Subadmin subadmin = subadminRepository.findById(subadminId)
        .orElseThrow(() -> new IllegalArgumentException("Subadmin not found with ID: " + subadminId));
    Session session = getSessionForSubadmin(subadmin);

    session.setDebug(true);
    MimeMessage m = new MimeMessage(session);

    try {
      m.setFrom(new InternetAddress(subadmin.getEmail()));
      m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
      m.setSubject(subject, "UTF-8");
      m.setContent(message, "text/html; charset=UTF-8");

      Transport.send(m);
      f = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }

  // Overloaded method: Send HTML email (defaults to Master Admin for
  // compatibility with existing calls)
  public boolean sendHtmlEmail(String message, String subject, String to) {
    boolean f = false;
    Session session = getSessionForMasterAdmin(); // Use Master Admin by default

    session.setDebug(true);
    MimeMessage m = new MimeMessage(session);

    try {
      MasterAdmin masterAdmin = masterAdminRepo.findFirstByRoll("MASTER_ADMIN");
      if (masterAdmin == null) {
        throw new IllegalStateException("Master Admin not found");
      }
      m.setFrom(new InternetAddress(masterAdmin.getEmail()));
      m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
      m.setSubject(subject, "UTF-8");
      m.setContent(message, "text/html; charset=UTF-8");

      Transport.send(m);
      f = true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }

  // Send email with attachment using Subadmin's credentials
  public boolean sendWithAttachment(int subadminId, String to, String subject, String message,
      MultipartFile attachmentFile) {
    boolean success = false;
    Subadmin subadmin = subadminRepository.findById(subadminId)
        .orElseThrow(() -> new IllegalArgumentException("Subadmin not found with ID: " + subadminId));
    Session session = getSessionForSubadmin(subadmin);
    session.setDebug(true);

    try {
      MimeMessage mimeMessage = new MimeMessage(session);
      mimeMessage.setFrom(new InternetAddress(subadmin.getEmail()));
      mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
      mimeMessage.setSubject(subject);

      MimeMultipart multipart = new MimeMultipart();

      MimeBodyPart textBodyPart = new MimeBodyPart();
      textBodyPart.setText(message);
      multipart.addBodyPart(textBodyPart);

      MimeBodyPart attachmentBodyPart = new MimeBodyPart();
      File attachmentFile2 = convertMultipartFileToFile(attachmentFile);
      attachmentBodyPart.attachFile(attachmentFile2);
      attachmentBodyPart.setFileName(attachmentFile.getOriginalFilename());
      multipart.addBodyPart(attachmentBodyPart);

      mimeMessage.setContent(multipart);

      Transport.send(mimeMessage);
      success = true;

      if (attachmentFile2 != null && attachmentFile2.exists()) {
        attachmentFile2.delete();
      }
    } catch (MessagingException | IOException e) {
      e.printStackTrace();
    }
    return success;
  }

  // Overloaded method: Send email with attachment (for compatibility, but
  // requires subadminId elsewhere)
  public boolean sendWithAttachment(String to, String subject, String message, MultipartFile attachmentFile) {
    // This is a fallback, but ideally, calling code should provide subadminId.
    // For now, throw an exception to enforce correct usage elsewhere.
    throw new IllegalArgumentException("Subadmin ID is required to send emails with attachments");
  }

  // Helper method to convert MultipartFile to File
  private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
    File file = File.createTempFile("attachment", "_" + multipartFile.getOriginalFilename());
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(multipartFile.getBytes());
    }
    return file;
  }
}