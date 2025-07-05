// package com.jaywant.demo.Controller;

// import com.jaywant.demo.Entity.Certificate;
// import com.jaywant.demo.Entity.Employee;
// import com.jaywant.demo.Service.CertificateService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;
// import java.util.List;

// @RestController
// @RequestMapping("/api/certificate")
// public class CertificateController {

//   @Autowired
//   private CertificateService certificateService;

//   // Upload certificate (joining letter, offer letter, termination letter)
//   @PostMapping("/upload/{subadminId}/{employeeFullName}")
//   public ResponseEntity<String> uploadCertificate(@PathVariable int subadminId,
//       @PathVariable String employeeFullName,
//       @RequestParam(value = "joiningLetter", required = false) MultipartFile joiningLetter,
//       @RequestParam(value = "offerLetter", required = false) MultipartFile offerLetter,
//       @RequestParam(value = "terminationLetter", required = false) MultipartFile terminationLetter) {

//     try {
//       Certificate certificate = certificateService.uploadCertificate(subadminId, employeeFullName, joiningLetter,
//           offerLetter, terminationLetter);
//       if (certificate == null) {
//         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
//       }
//       return ResponseEntity.ok("Certificate uploaded successfully");
//     } catch (IOException e) {
//       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
//     }
//   }

//   // Get certificates for a specific employee
//   @GetMapping("/get/{subadminId}/{employeeFullName}")
//   public ResponseEntity<List<Certificate>> getCertificates(@PathVariable int subadminId,
//       @PathVariable String employeeFullName) {
//     Employee employee = certificateService.getEmployee(subadminId, employeeFullName);
//     if (employee == null) {
//       return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//     }

//     List<Certificate> certificates = employee.getCertificates();
//     if (certificates.isEmpty()) {
//       return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//     }

//     return ResponseEntity.ok(certificates);
//   }

//   // Update certificate details (e.g., offer letter, termination letter)
//   @PutMapping("/update/{subadminId}/{employeeFullName}/{certificateId}")
//   public ResponseEntity<String> updateCertificate(@PathVariable int subadminId,
//       @PathVariable String employeeFullName,
//       @PathVariable Long certificateId,
//       @RequestParam(value = "joiningLetter", required = false) MultipartFile joiningLetter,
//       @RequestParam(value = "offerLetter", required = false) MultipartFile offerLetter,
//       @RequestParam(value = "terminationLetter", required = false) MultipartFile terminationLetter) {

//     try {
//       // Ensure the employee exists before updating the certificate
//       Employee employee = certificateService.getEmployee(subadminId, employeeFullName);
//       if (employee == null) {
//         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
//       }

//       // Call the service method to update the certificate
//       Certificate certificate = certificateService.updateCertificate(certificateId, joiningLetter, offerLetter,
//           terminationLetter);
//       if (certificate == null) {
//         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificate not found");
//       }
//       return ResponseEntity.ok("Certificate updated successfully");
//     } catch (IOException e) {
//       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File update failed: " + e.getMessage());
//     }
//   }

//   // Delete a certificate by certificateId and employee details
//   @DeleteMapping("/delete/{subadminId}/{employeeFullName}/{certificateId}")
//   public ResponseEntity<String> deleteCertificate(@PathVariable int subadminId,
//       @PathVariable String employeeFullName,
//       @PathVariable Long certificateId) {
//     try {
//       // Ensure the employee exists before deleting the certificate
//       Employee employee = certificateService.getEmployee(subadminId, employeeFullName);
//       if (employee == null) {
//         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found");
//       }

//       // Call the service method to delete the certificate
//       certificateService.deleteCertificate(certificateId);
//       return ResponseEntity.ok("Certificate deleted successfully");
//     } catch (Exception e) {
//       return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificate not found");
//     }
//   }
// }

package com.jaywant.demo.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Certificate;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Service.CertificateService;
import com.jaywant.demo.Service.EmailService;

// Temporarily disabled to fix startup issue
@RestController
@RequestMapping("/api/certificate")
@CrossOrigin(origins = "*")
public class CertificateController {

  @Autowired
  private CertificateService certificateService;

  @Autowired
  private EmailService emailService;

  // Upload document and send email if employee has email
  @PostMapping("/send/{subadminId}/{empId}/{documentType}")
  public ResponseEntity<?> sendDocument(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @PathVariable String documentType,
      @RequestParam("file") MultipartFile file) {

    try {
      // Get employee to check email
      Employee employee = certificateService.getEmployee(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found"));
      }

      // Define email subject and message based on document type
      String subject = getEmailSubject(documentType);
      String message = getEmailMessage(documentType, employee.getFullName());

      Map<String, Object> result = new HashMap<>();

      // Save file to disk (always do this even if no email exists)
      String filePath = certificateService.saveFileToDisk(subadminId, employee, file, documentType);
      result.put("filePath", filePath);

      // Check if employee has email
      if (employee.getEmail() != null && !employee.getEmail().isEmpty()) {
        // Send email with attachment using Subadmin's credentials
        boolean emailSent = emailService.sendWithAttachment(
            subadminId,
            employee.getEmail(),
            subject,
            message,
            file);

        // Save to database only if employee has email
        Certificate certificate = certificateService.saveCertificateToDatabase(
            employee,
            subadminId,
            documentType,
            filePath);

        result.put("certificateId", certificate.getId());
        result.put("emailSent", emailSent);
        result.put("email", employee.getEmail());
        result.put("storedInDatabase", true);
      } else {
        // No email found, only saved to file system
        result.put("emailSent", false);
        result.put("storedInDatabase", false);
        result.put("message", "Document saved to file system only. No email found for employee.");
      }

      return ResponseEntity.ok(result);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error processing document: " + e.getMessage()));
    }
  }

  // Upload multiple documents at once
  @PostMapping("/send-multiple/{subadminId}/{empId}")
  public ResponseEntity<?> sendMultipleDocuments(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestParam(value = "joiningLetter", required = false) MultipartFile joiningLetter,
      @RequestParam(value = "offerLetter", required = false) MultipartFile offerLetter,
      @RequestParam(value = "terminationLetter", required = false) MultipartFile terminationLetter,
      @RequestParam(value = "experienceLetter", required = false) MultipartFile experienceLetter,
      @RequestParam(value = "relievingLetter", required = false) MultipartFile relievingLetter) {

    try {
      // Get employee to check email
      Employee employee = certificateService.getEmployee(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found"));
      }

      Map<String, Object> result = new HashMap<>();

      // Process each document
      if (joiningLetter != null && !joiningLetter.isEmpty()) {
        processDocument(subadminId, employee, joiningLetter, "joining", result);
      }

      if (offerLetter != null && !offerLetter.isEmpty()) {
        processDocument(subadminId, employee, offerLetter, "appointment", result);
      }

      if (terminationLetter != null && !terminationLetter.isEmpty()) {
        processDocument(subadminId, employee, terminationLetter, "termination", result);
      }

      if (experienceLetter != null && !experienceLetter.isEmpty()) {
        processDocument(subadminId, employee, experienceLetter, "experience", result);
      }

      if (relievingLetter != null && !relievingLetter.isEmpty()) {
        processDocument(subadminId, employee, relievingLetter, "relieving", result);
      }

      if (result.isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "No files were provided"));
      }

      return ResponseEntity.ok(result);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error processing documents: " + e.getMessage()));
    }
  }

  // Helper method to process individual documents
  private void processDocument(int subadminId, Employee employee, MultipartFile file,
      String documentType, Map<String, Object> result) throws IOException {
    // Save file to disk (always do this)
    String filePath = certificateService.saveFileToDisk(subadminId, employee, file, documentType);

    Map<String, Object> docResult = new HashMap<>();
    docResult.put("filePath", filePath);

    // Send email if employee has email
    if (employee.getEmail() != null && !employee.getEmail().isEmpty()) {
      String subject = getEmailSubject(documentType);
      String message = getEmailMessage(documentType, employee.getFullName());

      // Send email with attachment using Subadmin's credentials
      boolean emailSent = emailService.sendWithAttachment(
          subadminId,
          employee.getEmail(),
          subject,
          message,
          file);

      // Save to database only if employee has email
      Certificate certificate = certificateService.saveCertificateToDatabase(
          employee,
          subadminId,
          documentType,
          filePath);

      docResult.put("certificateId", certificate.getId());
      docResult.put("emailSent", emailSent);
      docResult.put("storedInDatabase", true);
    } else {
      docResult.put("emailSent", false);
      docResult.put("storedInDatabase", false);
      docResult.put("message", "Document saved to file system only. No email found for employee.");
    }

    result.put(documentType, docResult);
  }

  // Get email subject based on document type
  private String getEmailSubject(String documentType) {
    switch (documentType) {
      case "joining":
        return "Your Joining Letter";
      case "appointment":
        return "Your Appointment Letter";
      case "termination":
        return "Termination Letter - Important Information";
      case "experience":
        return "Your Experience Certificate";
      case "relieving":
        return "Your Relieving Letter";
      case "increment":
        return "Salary Increment Information";
      case "agreement":
        return "Employment Agreement Document";
      case "performance":
        return "Performance Certificate";
      case "achievement":
        return "Achievement Certificate";
      case "internship":
        return "Internship Completion Certificate";
      case "exit":
        return "Exit Letter";
      case "postAppraisal":
        return "Post Appraisal Certificate";
      default:
        return "Important Document From Your Organization";
    }
  }

  // Get email message based on document type
  private String getEmailMessage(String documentType, String employeeName) {
    String greeting = "Dear " + employeeName + ",\n\n";
    String closing = "\n\nThank you,\nHR Department";

    switch (documentType) {
      case "joining":
        return greeting + "Please find attached your Joining Letter. We are excited to have you join our team!"
            + closing;
      case "appointment":
        return greeting + "Your Appointment Letter is attached. Please review all details carefully." + closing;
      case "termination":
        return greeting
            + "Please find attached an important document regarding your employment status. We regret to inform you about this decision."
            + closing;
      case "experience":
        return greeting
            + "As requested, please find attached your Experience Certificate. Thank you for your service with us."
            + closing;
      case "relieving":
        return greeting + "Your Relieving Letter is attached. Thank you for your contributions to the organization."
            + closing;
      case "increment":
        return greeting + "Congratulations! Please find attached your salary increment letter." + closing;
      case "agreement":
        return greeting + "Please find attached your Employment Agreement. Please review and sign the document."
            + closing;
      case "performance":
        return greeting + "Congratulations on your excellent performance! Your Performance Certificate is attached."
            + closing;
      case "achievement":
        return greeting + "Congratulations on your achievement! Your certificate is attached." + closing;
      case "internship":
        return greeting + "Congratulations on completing your internship! Your certificate is attached." + closing;
      case "exit":
        return greeting + "Please find attached your Exit Letter. We appreciate your service with us." + closing;
      case "postAppraisal":
        return greeting
            + "Please find attached your Post Appraisal Certificate following your recent performance review."
            + closing;
      default:
        return greeting + "Please find attached an important document for your reference." + closing;
    }
  }

  // Get certificates for a specific employee
  @GetMapping("/get/{subadminId}/{empId}")
  public ResponseEntity<?> getCertificates(@PathVariable int subadminId,
      @PathVariable int empId) {
    try {
      Employee employee = certificateService.getEmployee(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found"));
      }

      List<Certificate> certificates = certificateService.getCertificatesByEmployee(employee);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("employee", Map.of(
          "empId", employee.getEmpId(),
          "fullName", employee.getFullName(),
          "email", employee.getEmail(),
          "department", employee.getDepartment(),
          "jobRole", employee.getJobRole()));
      response.put("totalCertificates", certificates.size());
      response.put("certificates", certificates);

      if (certificates.isEmpty()) {
        response.put("message", "No certificates found for this employee");
      }

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error retrieving certificates: " + e.getMessage()));
    }
  }

  // Get all certificates for all employees under a subadmin
  @GetMapping("/get/{subadminId}/all")
  public ResponseEntity<?> getAllCertificatesForSubadmin(@PathVariable int subadminId) {
    try {
      // Get all employees under this subadmin
      List<Employee> employees = certificateService.getEmployeesBySubadmin(subadminId);
      if (employees.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "No employees found for this subadmin"));
      }

      List<Map<String, Object>> employeeCertificates = new ArrayList<>();
      int totalCertificates = 0;

      for (Employee employee : employees) {
        List<Certificate> certificates = certificateService.getCertificatesByEmployee(employee);

        Map<String, Object> employeeData = new HashMap<>();
        employeeData.put("empId", employee.getEmpId());
        employeeData.put("fullName", employee.getFullName());
        employeeData.put("email", employee.getEmail());
        employeeData.put("department", employee.getDepartment());
        employeeData.put("jobRole", employee.getJobRole());
        employeeData.put("certificateCount", certificates.size());
        employeeData.put("certificates", certificates);

        employeeCertificates.add(employeeData);
        totalCertificates += certificates.size();
      }

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("subadminId", subadminId);
      response.put("totalEmployees", employees.size());
      response.put("totalCertificates", totalCertificates);
      response.put("employeeCertificates", employeeCertificates);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error retrieving certificates: " + e.getMessage()));
    }
  }

  // Get certificate summary for a subadmin (counts by document type)
  @GetMapping("/summary/{subadminId}")
  public ResponseEntity<?> getCertificateSummary(@PathVariable int subadminId) {
    try {
      List<Employee> employees = certificateService.getEmployeesBySubadmin(subadminId);
      if (employees.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "No employees found for this subadmin"));
      }

      Map<String, Integer> documentTypeCounts = new HashMap<>();
      int totalCertificates = 0;
      int employeesWithCertificates = 0;

      for (Employee employee : employees) {
        List<Certificate> certificates = certificateService.getCertificatesByEmployee(employee);
        if (!certificates.isEmpty()) {
          employeesWithCertificates++;
          totalCertificates += certificates.size();

          // Count document types (this would need to be implemented in the service)
          for (Certificate cert : certificates) {
            // Count each type of document in the certificate
            if (cert.getJoiningLetterPath() != null)
              documentTypeCounts.merge("joining", 1, Integer::sum);
            if (cert.getAppointmentLetterPath() != null)
              documentTypeCounts.merge("appointment", 1, Integer::sum);
            if (cert.getExperienceLetterPath() != null)
              documentTypeCounts.merge("experience", 1, Integer::sum);
            if (cert.getRelievingLetterPath() != null)
              documentTypeCounts.merge("relieving", 1, Integer::sum);
            if (cert.getTerminationLetterPath() != null)
              documentTypeCounts.merge("termination", 1, Integer::sum);
            if (cert.getIncrementLetterPath() != null)
              documentTypeCounts.merge("increment", 1, Integer::sum);
            if (cert.getAgreementContractPath() != null)
              documentTypeCounts.merge("agreement", 1, Integer::sum);
            if (cert.getPerformanceCertificatePath() != null)
              documentTypeCounts.merge("performance", 1, Integer::sum);
            if (cert.getAchievementCertificatePath() != null)
              documentTypeCounts.merge("achievement", 1, Integer::sum);
            if (cert.getInternshipCompletionPath() != null)
              documentTypeCounts.merge("internship", 1, Integer::sum);
            if (cert.getPostAppraisalPath() != null)
              documentTypeCounts.merge("postAppraisal", 1, Integer::sum);
          }
        }
      }

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("subadminId", subadminId);
      response.put("totalEmployees", employees.size());
      response.put("employeesWithCertificates", employeesWithCertificates);
      response.put("totalCertificates", totalCertificates);
      response.put("documentTypeCounts", documentTypeCounts);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error generating certificate summary: " + e.getMessage()));
    }
  }

  // Delete a certificate
  @DeleteMapping("/delete/{subadminId}/{empId}/{certificateId}")
  public ResponseEntity<?> deleteCertificate(@PathVariable int subadminId,
      @PathVariable int empId,
      @PathVariable Long certificateId) {
    try {
      // Ensure the employee exists before deleting the certificate
      Employee employee = certificateService.getEmployee(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found"));
      }

      // Call the service method to delete the certificate
      certificateService.deleteCertificate(certificateId);
      return ResponseEntity.ok(Map.of("message", "Certificate deleted successfully"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error deleting certificate: " + e.getMessage()));
    }
  }
}