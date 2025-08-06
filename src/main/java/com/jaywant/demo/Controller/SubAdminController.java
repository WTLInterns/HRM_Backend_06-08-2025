package com.jaywant.demo.Controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.MasterAdmin;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Exception.ResourceNotFoundException;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.EmailService;
import com.jaywant.demo.Service.SubAdminPasswordResetService;
import com.jaywant.demo.Service.SubAdminService;
import com.jaywant.demo.Service.AttendanceService;
import com.jaywant.demo.Service.ExcelService;

@RestController
@RequestMapping("/api/subadmin")
public class SubAdminController {

  @Autowired
  private SubAdminRepo subAdminRepo;

  @Autowired
  private EmailService emailService;

  @Autowired
  private SubAdminPasswordResetService passwordResetService;

  @Autowired
  private EmployeeRepo employeeRepo;

  @Autowired
  private SubAdminService subAdminService;

  @Autowired
  private AttendanceService attendanceService;

  @Autowired
  private ExcelService excelService;

  // Endpoint for SubAdmin login
  @PostMapping("/login")

  public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
    try {
      List<Subadmin> subAdmins = subAdminRepo.findByEmail(email);
      if (subAdmins.isEmpty()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
      }
      Subadmin subAdmin = subAdmins.get(0);
      // Check password
      if (!password.equals(subAdmin.getPassword())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
      }
      // Check active status
      if (!"Active".equalsIgnoreCase(subAdmin.getStatus())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account is inactive. Please contact support.");
      }
      // Build response with all subadmin info and its masteradmin info
      Map<String, Object> response = new HashMap<>();
      response.put("id", subAdmin.getId());
      response.put("name", subAdmin.getName());
      response.put("lastname", subAdmin.getLastname());
      response.put("stampImg", subAdmin.getStampImg());
      response.put("signature", subAdmin.getSignature());
      response.put("email", subAdmin.getEmail());
      response.put("phoneno", subAdmin.getPhoneno());
      response.put("password", subAdmin.getPassword());
      response.put("registercompanyname", subAdmin.getRegistercompanyname());
      response.put("companylogo", subAdmin.getCompanylogo());
      response.put("roll", subAdmin.getRoll());
      response.put("gstno", subAdmin.getGstno());
      response.put("status", subAdmin.getStatus());
      response.put("cinno", subAdmin.getCinno());
      response.put("companyurl", subAdmin.getCompanyurl());
      response.put("address", subAdmin.getAddress());
      response.put("latitude", subAdmin.getLatitude());
      response.put("longitude", subAdmin.getLongitude());
      response.put("packageType", subAdmin.getPackageType());
      response.put("packageCount", subAdmin.getPackageCount());
      MasterAdmin master = subAdmin.getMasterAdmin();
      if (master != null) {
        Map<String, Object> masterInfo = new HashMap<>();
        masterInfo.put("id", master.getId());
        masterInfo.put("name", master.getName());
        masterInfo.put("email", master.getEmail());
        masterInfo.put("mobileno", master.getMobileno());
        masterInfo.put("roll", master.getRoll());
        masterInfo.put("profileImg", master.getProfileImg());
        response.put("masterAdmin", masterInfo);
      } else {
        response.put("masterAdmin", null);
      }
      return ResponseEntity.ok(response);
    } catch (Exception ex) {
      ex.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred during login.");
    }
  }

  // @GetMapping("/get-subadmin/{id}")
  // public Subadmin getSubAdminStatusById(@PathVariable int id) {

  // return this.subAdminRepo.findByStatusSubadminId(id);

  // }

  // Endpoint for updating SubAdmin fields including status and image files.
  // @PutMapping("/update-fields/{id}")
  // public ResponseEntity<?> updateSubAdminFields(
  // @PathVariable int id,
  // @RequestParam String name,
  // @RequestParam String lastname,
  // @RequestParam String email,
  // @RequestParam String phoneno,
  // @RequestParam String registercompanyname,
  // @RequestParam(value = "stampImg", required = false) MultipartFile stampImg,
  // @RequestParam(value = "signature", required = false) MultipartFile signature,
  // @RequestParam(value = "companylogo", required = false) MultipartFile
  // companylogo,
  // @RequestParam String status,
  // @RequestParam String companyurl,
  // @RequestParam String address,
  // @RequestParam String cinno) {
  // try {
  // Optional<Subadmin> subAdminOpt = subAdminRepo.findById(id);
  // if (!subAdminOpt.isPresent()) {
  // return ResponseEntity.status(HttpStatus.NOT_FOUND).body("SubAdmin with ID " +
  // id + " not found.");
  // }
  // Subadmin subAdmin = subAdminOpt.get();
  // subAdmin.setName(name);
  // subAdmin.setLastname(lastname);
  // subAdmin.setEmail(email);
  // subAdmin.setPhoneno(phoneno);
  // subAdmin.setRegistercompanyname(registercompanyname);
  // subAdmin.setStatus(status);
  // subAdmin.setAddress(address);
  // subAdmin.setCinno(cinno);
  // subAdmin.setCompanyurl(companyurl);

  // // Update file fields only if provided
  // if (stampImg != null && !stampImg.isEmpty())
  // subAdmin.setStampImg(stampImg.getOriginalFilename());
  // if (signature != null && !signature.isEmpty())
  // subAdmin.setSignature(signature.getOriginalFilename());
  // if (companylogo != null && !companylogo.isEmpty())
  // subAdmin.setCompanylogo(companylogo.getOriginalFilename());

  // Subadmin updatedSubAdmin = subAdminRepo.save(subAdmin);
  // return ResponseEntity.ok(updatedSubAdmin);
  // } catch (RuntimeException ex) {
  // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  // }
  // }

  @PutMapping(value = "/update-fields/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> updateSubAdminFields(
      @PathVariable int id,
      @RequestParam String name,
      @RequestParam String lastname,
      @RequestParam String email,
      @RequestParam String phoneno,
      @RequestParam String registercompanyname,
      @RequestParam String status,
      @RequestParam String companyurl,
      @RequestParam String address,
      @RequestParam String cinno,
      @RequestParam(required = false) String gstno,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude,
      @RequestParam(value = "stampImg", required = false) MultipartFile stampImg,
      @RequestParam(value = "signature", required = false) MultipartFile signature,
      @RequestParam(value = "companylogo", required = false) MultipartFile companylogo,
      @RequestParam(value = "packageType", required = false) String packageType,
      @RequestParam(value = "customCount", required = false) Integer customCount,
      @RequestParam(value = "emailServerPassword", required = false) String emailServerPassword,
      @RequestParam(value = "deviceSerialNumber", required = false) String deviceSerialNumber) {
    Optional<Subadmin> existing = subAdminRepo.findById(id);
    if (!existing.isPresent()) {
      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body("SubAdmin with ID " + id + " not found.");
    }

    try {
      Subadmin updated = subAdminService.updateSubAdmin(
          id,
          name, lastname, email, phoneno,
          registercompanyname,
          status,
          gstno,
          cinno,
          companyurl,
          address,
          stampImg,
          signature,
          companylogo, latitude, longitude,
          packageType, customCount, emailServerPassword, deviceSerialNumber);
      return ResponseEntity.ok(updated);

    } catch (RuntimeException ex) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body("Update failed: " + ex.getMessage());
    }
  }

  // Endpoint for updating SubAdmin status
  @PutMapping(value = "/update-status/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> updateSubAdminStatus(@PathVariable int id, @RequestParam String status) {
    try {
      Optional<Subadmin> subAdminOpt = subAdminRepo.findById(id);
      if (!subAdminOpt.isPresent()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("SubAdmin with ID " + id + " not found.");
      }
      Subadmin subAdmin = subAdminOpt.get();
      subAdmin.setStatus(status);
      subAdminRepo.save(subAdmin);
      return ResponseEntity.ok("SubAdmin status updated successfully.");
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating status.");
    }
  }

  // Endpoint to send email to sub-admin (dummy implementation)
  @PostMapping("/send-email/{subadminemail}")
  public String sendEmail(@PathVariable String subadminemail) {
    List<Subadmin> s = subAdminRepo.findByEmail(subadminemail);
    if (s == null || s.isEmpty()) {
      return "Subadmin not found";
    }
    Subadmin subadmin = s.get(0);
    String message = "<html>" +
        "<body style='font-family: Arial, sans-serif; background-color: #f0f2f5; padding: 30px; color: #333;'>" +
        "<div style='max-width: 600px; margin: auto; background-color: #ffffff; padding: 30px; border-radius: 12px; box-shadow: 0 0 10px rgba(0,0,0,0.1);'>"
        +
        "<h2 style='color: #2c3e50; margin-bottom: 10px;'>Hello " + subadmin.getName() + ",</h2>" +
        "<p style='font-size: 16px; margin: 15px 0;'><strong>Welcome to WTL Tourism Pvt. Ltd.!</strong></p>" +
        "<p style='font-size: 15px; margin: 15px 0;'>We're excited to have you on board as an <strong>HRM-Sub-Admin</strong>. Your account has been created successfully. Below are your login credentials:</p>"
        +
        "<p style='font-size: 15px; margin: 15px 0;'><strong>Email:</strong> " + subadmin.getEmail() + "<br>" +
        "<strong>Password:</strong> " + subadmin.getPassword() + "</p>" +
        "<p style='font-size: 15px; margin: 15px 0;'>Login URL: <a href='https://managifyhr.com' style='color: #007bff; text-decoration: none;'>https://managifyhr.com</a></p>"
        +
        "<p style='font-size: 15px; margin: 15px 0;'><strong>Please change your password after your first login</strong> to ensure account security.</p>"
        +
        "<p style='font-size: 15px; margin: 15px 0;'>If you face any issues, feel free to contact our support team at any time.</p>"
        +
        "<p style='font-size: 15px; margin-top: 30px;'>Thanks & Regards,<br><strong>Team WTL Tourism Pvt. Ltd.</strong></p>"
        +
        "</div>" +
        "</body>" +
        "</html>";
    String to = subadminemail;
    String subject = "Your HRM-Sub-Admin Account Details";
    boolean sent = emailService.sendHtmlEmail(message, subject, to);
    return sent ? "Sent" : "Not Sent";
  }

  // Endpoint to fetch all sub-admins by company (using registercompanyname)
  @GetMapping("/by-company/{registercompanyname:.+}")
  public ResponseEntity<?> getSubAdminsByCompanyName(@PathVariable("registercompanyname") String companyName) {
    try {
      List<Subadmin> subAdmins = subAdminRepo.findByRegistercompanyname(companyName);
      if (subAdmins.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No sub-admins found for company: " + companyName);
      }
      return ResponseEntity.ok(subAdmins);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while fetching sub-admins.");
    }
  }

  // Endpoint for forgot password request (send OTP)
  @PostMapping("/forgot-password/request")
  public ResponseEntity<String> requestForgotPassword(@RequestParam String email) {
    try {
      passwordResetService.sendResetOTP(email); // Triggers OTP email using Master Admin's email
      return ResponseEntity.ok("OTP sent to email: " + email);
    } catch (RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
  }

  // Endpoint for verifying OTP and resetting password
  @PostMapping("/forgot-password/verify")
  public ResponseEntity<String> verifyOtpAndResetPassword(
      @RequestParam String email,
      @RequestParam String otp,
      @RequestParam String newPassword) {
    try {
      if (!passwordResetService.verifyOTP(email, otp)) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP.");
      }
      passwordResetService.resetPassword(email, newPassword);
      return ResponseEntity.ok("Password updated successfully.");
    } catch (RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
  }

  // Endpoint to retrieve sub-admin details by email
  @GetMapping("/subadmin-by-email/{email}")
  public ResponseEntity<?> getSubAdminByEmail(@PathVariable("email") String email) {
    List<Subadmin> subAdmins = subAdminRepo.findByEmail(email);
    if (subAdmins.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("SubAdmin with email " + email + " not found.");
    }
    return ResponseEntity.ok(subAdmins.get(0));
  }

  // --- New Endpoint: Add Employee under a Subadmin ---
  // @PostMapping("/add-employee/{subadminId}")
  // public ResponseEntity<?> addEmployee(
  // @PathVariable("subadminId") int subadminId,
  // @RequestParam String firstName,
  // @RequestParam String lastName,
  // @RequestParam String email,
  // @RequestParam Long phone,
  // @RequestParam String aadharNo,
  // @RequestParam String panCard,
  // @RequestParam String education,
  // @RequestParam String bloodGroup,
  // @RequestParam String jobRole,
  // @RequestParam String gender,
  // @RequestParam String address,
  // @RequestParam String birthDate,
  // @RequestParam String joiningDate,
  // @RequestParam String status,
  // @RequestParam String bankName,
  // @RequestParam String bankAccountNo,
  // @RequestParam String bankIfscCode,
  // @RequestParam String branchName,
  // @RequestParam Long salary) {
  // try {
  // // Find the subadmin by id
  // Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
  // if (!subadminOpt.isPresent()) {
  // return ResponseEntity.status(HttpStatus.NOT_FOUND)
  // .body("Subadmin with ID " + subadminId + " not found.");
  // }
  // Subadmin subadmin = subadminOpt.get();

  // // Create and map Employee fields
  // Employee employee = new Employee();
  // employee.setFirstName(firstName);
  // employee.setLastName(lastName);
  // employee.setEmail(email);
  // employee.setPhone(phone);
  // employee.setAadharNo(aadharNo);
  // employee.setPanCard(panCard);
  // employee.setEducation(education);
  // employee.setBloodGroup(bloodGroup);
  // employee.setJobRole(jobRole);
  // employee.setGender(gender);
  // employee.setAddress(address);
  // employee.setBirthDate(birthDate);
  // employee.setJoiningDate(joiningDate);
  // employee.setStatus(status);
  // employee.setBankName(bankName);
  // employee.setBankAccountNo(bankAccountNo);
  // employee.setBankIfscCode(bankIfscCode);
  // employee.setBranchName(branchName);
  // employee.setSalary(salary);
  // employee.setRole("EMPLOYEE");

  // // Set the subadmin relationship
  // employee.setSubadmin(subadmin);

  // // Save the employee using employeeRepo
  // Employee savedEmployee = employeeRepo.save(employee);
  // return ResponseEntity.ok(savedEmployee);
  // } catch (Exception ex) {
  // return ResponseEntity.status(HttpStatus.BAD_REQUEST)
  // .body("Error while adding employee: " + ex.getMessage());
  // }
  // }

  @PostMapping(value = "/add-employee/{subadminId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> addEmployee(
      @PathVariable int subadminId,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phone,
      @RequestParam(required = false) String aadharNo,
      @RequestParam(required = false) String panCard,
      @RequestParam(required = false) String education,
      @RequestParam(required = false) String bloodGroup,
      @RequestParam(required = false) String jobRole,
      @RequestParam(required = false) String gender,
      @RequestParam(required = false) String address,
      @RequestParam(required = false) String birthDate,
      @RequestParam(required = false) String joiningDate,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String bankName,
      @RequestParam(required = false) String bankAccountNo,
      @RequestParam(required = false) String bankIfscCode,
      @RequestParam(required = false) String branchName,
      @RequestParam(required = false) Long salary,
      @RequestPart(required = false) MultipartFile empimg,
      @RequestPart(required = false) MultipartFile adharimg,
      @RequestPart(required = false) MultipartFile panimg,
      @RequestParam(required = false) String department) {

    try {
      // Convert phone string to Long
      Long phoneNumber = null;
      if (phone != null && !phone.trim().isEmpty()) {
        try {
          phoneNumber = Long.parseLong(phone.trim());
        } catch (NumberFormatException e) {
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body("Invalid phone number format: " + phone);
        }
      }

      // Check package limit
      Subadmin subadmin = subAdminRepo.findById(subadminId)
          .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + subadminId));
      int currentCount = employeeRepo.countBySubadmin(subadmin);
      if (subadmin.getPackageCount() != null && currentCount >= subadmin.getPackageCount()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Cannot add employee: Package limit exceeded. This subadmin can add only "
                + subadmin.getPackageCount() + " employees.");
      }
      Employee created = subAdminService.addEmployee(
          subadminId,
          firstName, lastName, email, phoneNumber, aadharNo, panCard,
          education, bloodGroup, jobRole, gender, address,
          birthDate, joiningDate, status, bankName,
          bankAccountNo, bankIfscCode, branchName, salary,
          empimg, adharimg, panimg, department);
      return ResponseEntity.ok(created);
    } catch (RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Error adding employee: " + ex.getMessage());
    }
  }

  // Endpoint to get all subadmins with details
  @GetMapping("/all")
  public ResponseEntity<?> getAllSubadmins() {
    try {
      List<Subadmin> subadmins = subAdminRepo.findAll();
      if (subadmins.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No subadmins found.");
      }
      return ResponseEntity.ok(subadmins);
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("An error occurred while fetching subadmins.");
    }
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<?> deleteSubAdmin(@PathVariable int id) {
    Optional<Subadmin> subAdminOpt = subAdminRepo.findById(id);
    if (!subAdminOpt.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("SubAdmin with ID " + id + " not found.");
    }
    subAdminRepo.deleteById(id);
    return ResponseEntity.ok("SubAdmin deleted successfully.");
  }

  /**
   * GET: Retrieve subadmin images (stampImg, signature, companyLogo) as Base64
   * strings.
   * URL: GET /api/subadmin/images/{id}
   */
  @GetMapping("/images/{id}")
  public ResponseEntity<?> getSubAdminImages(@PathVariable int id) {
    try {
      Subadmin subAdmin = subAdminRepo.findById(id)
          .orElseThrow(() -> new RuntimeException("Subadmin not found with id: " + id));
      // Use the same folder for reading images that is used for saving files.
      String uploadDir = "src/main/resources/static/images/profile/";
      Map<String, String> images = new HashMap<>();

      if (subAdmin.getStampImg() != null && !subAdmin.getStampImg().isEmpty()) {
        byte[] stampBytes = Files.readAllBytes(Paths.get(uploadDir + subAdmin.getStampImg()));
        String stampBase64 = Base64.getEncoder().encodeToString(stampBytes);
        images.put("stampImg", stampBase64);
      }
      if (subAdmin.getSignature() != null && !subAdmin.getSignature().isEmpty()) {
        byte[] sigBytes = Files.readAllBytes(Paths.get(uploadDir + subAdmin.getSignature()));
        String sigBase64 = Base64.getEncoder().encodeToString(sigBytes);
        images.put("signature", sigBase64);
      }
      if (subAdmin.getCompanylogo() != null && !subAdmin.getCompanylogo().isEmpty()) {
        byte[] logoBytes = Files.readAllBytes(Paths.get(uploadDir + subAdmin.getCompanylogo()));
        String logoBase64 = Base64.getEncoder().encodeToString(logoBytes);
        images.put("companyLogo", logoBase64);
      }
      return ResponseEntity.ok(images);
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error reading image files: " + e.getMessage());
    }
  }

  /**
   * PUT: Update the subadmin's images (stampImg, signature, companyLogo).
   * URL: PUT /api/subadmin/images/{id}
   */
  @PutMapping("/images/{id}")
  public ResponseEntity<?> updateSubAdminImages(
      @PathVariable int id,
      @RequestParam(value = "stampImg", required = false) MultipartFile stampImg,
      @RequestParam(value = "signature", required = false) MultipartFile signature,
      @RequestParam(value = "companylogo", required = false) MultipartFile companylogo) {
    try {
      Subadmin updatedSubadmin = subAdminService.updateSubAdminImages(id, stampImg, signature, companylogo);
      return ResponseEntity.ok(updatedSubadmin);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
  }

  // Endpoint to get only package info for a subadmin by email
  @GetMapping("/package-info/{email}")
  public ResponseEntity<?> getSubadminPackageInfo(@PathVariable("email") String email) {
    List<Subadmin> subAdmins = subAdminRepo.findByEmail(email);
    if (subAdmins.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("SubAdmin with email " + email + " not found.");
    }
    Subadmin subadmin = subAdmins.get(0);
    Map<String, Object> packageInfo = new HashMap<>();
    packageInfo.put("packageType", subadmin.getPackageType());
    packageInfo.put("packageCount", subadmin.getPackageCount());
    return ResponseEntity.ok(packageInfo);
  }

  // Endpoint to get only package info for a subadmin by id
  @GetMapping("/package-info-by-id/{id}")
  public ResponseEntity<?> getSubadminPackageInfoById(@PathVariable("id") int id) {
    Optional<Subadmin> subAdminOpt = subAdminRepo.findById(id);
    if (!subAdminOpt.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("SubAdmin with ID " + id + " not found.");
    }
    Subadmin subadmin = subAdminOpt.get();
    Map<String, Object> packageInfo = new HashMap<>();
    packageInfo.put("packageType", subadmin.getPackageType());
    packageInfo.put("packageCount", subadmin.getPackageCount());
    return ResponseEntity.ok(packageInfo);
  }

  @GetMapping("/status/{subadminId}")
  public ResponseEntity<Map<String, String>> getSubadminStatus(@PathVariable Integer subadminId) {
    Subadmin subadmin = subAdminRepo.findById(subadminId)
        .orElseThrow(() -> new ResourceNotFoundException("Subadmin not found"));
    Map<String, String> response = new HashMap<>();
    response.put("status", subadmin.getStatus());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{subadminId}/attendance/download")
  public ResponseEntity<byte[]> downloadAttendanceReport(
      @PathVariable int subadminId,
      @RequestParam String month) {
    try {
      List<com.jaywant.demo.Entity.Attendance> attendanceList = attendanceService.getAttendanceForSubAdminByMonth(subadminId, month);
      byte[] excelFile = excelService.generateAttendanceExcel(attendanceList);

      org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentDispositionFormData("attachment", "attendance_" + month + ".xlsx");

      return new ResponseEntity<>(excelFile, headers, HttpStatus.OK);
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
