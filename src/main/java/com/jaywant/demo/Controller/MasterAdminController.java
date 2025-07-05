package com.jaywant.demo.Controller;

import com.jaywant.demo.Entity.MasterAdmin;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Service.MasterAdminPasswordReset;
import com.jaywant.demo.Service.MasterAdminService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/masteradmin")
// @CrossOrigin(origins = "*")
public class MasterAdminController {

  @Autowired
  private MasterAdminService masterAdminService;

  @Autowired
  private MasterAdminPasswordReset passwordResetService;

  // ----- MasterAdmin Endpoints -----

  @PostMapping("/register")
  public ResponseEntity<?> registerMasterAdmin(
      @RequestParam("name") String name,
      @RequestParam("email") String email,
      @RequestParam("mobileno") long mobileno,
      @RequestParam("roll") String roll,
      @RequestParam("password") String password,
      @RequestParam(value = "profileImg", required = false) MultipartFile profileImg,
      @RequestParam(value = "MasterAdminEmailServerPassword", required = false) String masterAdminEmailServerPassword) {
    try {
      MasterAdmin masterAdmin = new MasterAdmin(mobileno, name, email, mobileno, roll, password,
          masterAdminEmailServerPassword);
      MasterAdmin saved = masterAdminService.registerMasterAdmin(masterAdmin, profileImg);
      return ResponseEntity.ok(saved);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Registration failed: " + e.getMessage());
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
    MasterAdmin admin = masterAdminService.login(email, password);
    return admin != null ? ResponseEntity.ok(admin)
        : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
  }

  @PutMapping(value = "/update", consumes = "multipart/form-data")
  public ResponseEntity<?> updateMasterAdmin(
      @RequestParam("id") Long id,
      @RequestParam("name") String name,
      @RequestParam("email") String email,
      @RequestParam("mobileno") long mobileno,
      @RequestParam("roll") String roll,
      @RequestParam("password") String password,
      @RequestParam(value = "profileImg", required = false) MultipartFile profileImg,
      @RequestParam(value = "MasterAdminEmailServerPassword", required = false) String masterAdminEmailServerPassword) {
    try {
      MasterAdmin masterAdmin = new MasterAdmin(id, name, email, mobileno, roll, password,
          masterAdminEmailServerPassword);
      MasterAdmin updated = masterAdminService.updateMasterAdmin(masterAdmin, profileImg);
      return ResponseEntity.ok(updated);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Update failed: " + e.getMessage());
    }
  }

  @GetMapping("/find")
  public ResponseEntity<?> findByEmail(@RequestParam String email) {
    MasterAdmin admin = masterAdminService.findByEmail(email);
    return admin != null ? ResponseEntity.ok(admin)
        : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Master Admin not found");
  }

  @DeleteMapping("/delete/{id}")
  public ResponseEntity<?> deleteMasterAdmin(@PathVariable Long id) {
    try {
      masterAdminService.deleteMasterAdmin(id);
      return ResponseEntity.ok("Master Admin deleted successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Delete failed: " + e.getMessage());
    }
  }

  @PostMapping("/update-password")
  public ResponseEntity<?> updatePassword(@RequestParam Long id, @RequestParam String newPassword) {
    try {
      masterAdminService.updatePassword(id, newPassword);
      return ResponseEntity.ok("Password updated successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password update failed: " + e.getMessage());
    }
  }

  @PostMapping("/forgot-password/request")
  public ResponseEntity<?> requestForgotPassword(@RequestParam String email) {
    try {
      passwordResetService.sendResetOTP(email);
      return ResponseEntity.ok("OTP sent to email: " + email);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP request failed: " + e.getMessage());
    }
  }

  @PostMapping("/forgot-password/verify")
  public ResponseEntity<?> verifyOtpAndResetPassword(
      @RequestParam String email,
      @RequestParam String otp,
      @RequestParam String newPassword) {
    if (!passwordResetService.verifyOTP(email, otp)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP");
    }
    try {
      passwordResetService.resetPassword(email, newPassword);
      return ResponseEntity.ok("Password reset successful");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password reset failed: " + e.getMessage());
    }
  }

  @PostMapping("/reg")
  public String testRegister() {
    return "Register endpoint is working!";
  }

  // ----- Subadmin Endpoint -----
  // The endpoint now accepts multipart/form-data for file uploads

  @PostMapping(value = "/addSubAdmin/{id}", consumes = "multipart/form-data")
  public ResponseEntity<?> addSubAdmin(
      @PathVariable Long id,
      @RequestParam("name") String name,
      @RequestParam("lastname") String lastname,
      @RequestParam("email") String email,
      @RequestParam("phoneno") String phoneno,
      @RequestParam("password") String password,
      @RequestParam("registercompanyname") String registercompanyname,
      @RequestParam("gstno") String gstno,
      @RequestParam("status") String status,
      @RequestParam(value = "stampImg", required = false) MultipartFile stampImg,
      @RequestParam(value = "signature", required = false) MultipartFile signature,
      @RequestParam(value = "companylogo", required = false) MultipartFile companylogo,
      @RequestParam(value = "cinno", required = false) String cinno,
      @RequestParam(value = "address", required = false) String address,
      @RequestParam(value = "companyurl", required = false) String companyurl,
      @RequestParam(value = "latitude", required = false) Double latitude,
      @RequestParam(value = "longitude", required = false) Double longitude,
      @RequestParam("packageType") String packageType,
      @RequestParam(value = "customCount", required = false) Integer customCount,
      @RequestParam("emailServerPassword") String emailServerPassword) {
    try {
      Subadmin subadmin = new Subadmin();
      subadmin.setName(name);
      subadmin.setLastname(lastname);
      subadmin.setEmail(email);
      subadmin.setPhoneno(phoneno);
      subadmin.setPassword(password);
      subadmin.setRegistercompanyname(registercompanyname);
      subadmin.setGstno(gstno);
      subadmin.setStatus(status);
      subadmin.setAddress(address);
      subadmin.setCinno(cinno);
      subadmin.setCompanyurl(companyurl);
      subadmin.setLatitude(latitude);
      subadmin.setLongitude(longitude);
      // Role will be set by default in the entity ("SUB_ADMIN")

      Subadmin saved = masterAdminService.createSubAdmin(subadmin, id, stampImg, signature, companylogo, packageType,
          customCount, emailServerPassword);
      return ResponseEntity.ok(saved);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("SubAdmin registration failed: " + e.getMessage());
    }
  }

  @GetMapping("/profileImg")
  public ResponseEntity<byte[]> getProfileImage(@RequestParam String email) {
    try {
      byte[] imageBytes = masterAdminService.getProfileImage(email);
      return ResponseEntity.ok()
          .contentType(MediaType.IMAGE_JPEG) // Or determine dynamically based on file extension
          .body(imageBytes);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(null);
    }
  }

}

// package com.jaywant.demo.Controller;

// import com.jaywant.demo.Entity.MasterAdmin;
// import com.jaywant.demo.Service.MasterAdminPasswordReset;
// import com.jaywant.demo.Service.MasterAdminService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;

// @RestController
// @RequestMapping("/masteradmin")
// @CrossOrigin(origins = "*")
// public class MasterAdminController {

// @Autowired
// private MasterAdminService masterAdminService;

// @Autowired
// private MasterAdminPasswordReset passwordResetService;

// // Registration endpoint (supports form-data)
// @PostMapping("/register")
// public ResponseEntity<?> registerMasterAdmin(
// @RequestParam("name") String name,
// @RequestParam("email") String email,
// @RequestParam("mobileno") long mobileno,
// @RequestParam("roll") String roll,
// @RequestParam("password") String password,
// @RequestParam(value = "profileImgFile", required = false) MultipartFile
// profileImgFile) {
// try {
// // Either use a parameterized constructor or create an instance and set the
// // fields.
// MasterAdmin masterAdmin = new MasterAdmin();
// masterAdmin.setName(name);
// masterAdmin.setEmail(email);
// masterAdmin.setMobileno(mobileno);
// masterAdmin.setRoll(roll);
// masterAdmin.setPassword(password);

// MasterAdmin saved = masterAdminService.registerMasterAdmin(masterAdmin,
// profileImgFile);
// return ResponseEntity.ok(saved);
// } catch (Exception e) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
// .body("Registration failed: " + e.getMessage());
// }
// }

// // Login endpoint
// @PostMapping("/login")
// public ResponseEntity<?> login(@RequestParam String email, @RequestParam
// String password) {
// MasterAdmin admin = masterAdminService.login(email, password);
// return (admin != null)
// ? ResponseEntity.ok(admin)
// : ResponseEntity.status(HttpStatus.UNAUTHORIZED)
// .body("Invalid email or password");
// }

// // Update MasterAdmin (update all fields, supports form-data)
// @PutMapping(value = "/update", consumes = "multipart/form-data")
// public ResponseEntity<?> updateMasterAdmin(
// @ModelAttribute MasterAdmin masterAdmin,
// @RequestParam(value = "profileImgFile", required = false) MultipartFile
// profileImgFile) {
// try {
// MasterAdmin updated = masterAdminService.updateMasterAdmin(masterAdmin,
// profileImgFile);
// return ResponseEntity.ok(updated);
// } catch (Exception e) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
// .body("Update failed: " + e.getMessage());
// }
// }

// // Find MasterAdmin by email
// @GetMapping("/find")
// public ResponseEntity<?> findByEmail(@RequestParam String email) {
// MasterAdmin admin = masterAdminService.findByEmail(email);
// return (admin != null)
// ? ResponseEntity.ok(admin)
// : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Master Admin not found");
// }

// // Delete MasterAdmin by ID
// @DeleteMapping("/delete/{id}")
// public ResponseEntity<?> deleteMasterAdmin(@PathVariable Long id) {
// try {
// masterAdminService.deleteMasterAdmin(id);
// return ResponseEntity.ok("Master Admin deleted successfully");
// } catch (Exception e) {
// return ResponseEntity.status(HttpStatus.NOT_FOUND)
// .body("Delete failed: " + e.getMessage());
// }
// }

// // Update password for MasterAdmin
// @PostMapping("/update-password")
// public ResponseEntity<?> updatePassword(@RequestParam Long id, @RequestParam
// String newPassword) {
// try {
// masterAdminService.updatePassword(id, newPassword);
// return ResponseEntity.ok("Password updated successfully");
// } catch (Exception e) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
// .body("Password update failed: " + e.getMessage());
// }
// }

// // Request OTP for password reset for MasterAdmin
// @PostMapping("/forgot-password/request")
// public ResponseEntity<?> requestForgotPassword(@RequestParam String email) {
// try {
// passwordResetService.sendResetOTP(email);
// return ResponseEntity.ok("OTP sent to email: " + email);
// } catch (Exception e) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
// .body("OTP request failed: " + e.getMessage());
// }
// }

// // Verify OTP and reset password for MasterAdmin
// @PostMapping("/forgot-password/verify")
// public ResponseEntity<?> verifyOtpAndResetPassword(
// @RequestParam String email,
// @RequestParam String otp,
// @RequestParam String newPassword) {
// if (!passwordResetService.verifyOTP(email, otp)) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid OTP");
// }
// try {
// passwordResetService.resetPassword(email, newPassword);
// return ResponseEntity.ok("Password reset successful");
// } catch (Exception e) {
// return ResponseEntity.status(HttpStatus.BAD_REQUEST)
// .body("Password reset failed: " + e.getMessage());
// }
// }

// // Test Endpoint
// @PostMapping("/reg")
// public String testRegister() {
// return "Register endpoint is working!";
// }
// }
