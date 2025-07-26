// package com.jaywant.demo.Service;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
// import java.util.List;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.web.multipart.MultipartFile;
// import com.jaywant.demo.Entity.Subadmin;
// import com.jaywant.demo.Repo.SubAdminRepo;

// @Service
// public class SubAdminService {

//   @Autowired
//   private SubAdminRepo repo;

//   // Directory where all files (images) will be stored.
//   // If this folder does not exist, it will be automatically created.
//   private final String uploadDir = "src/main/resources/static/images/profile/";

//   // Method to return a Subadmin by email.
//   public Subadmin getSubAdminByEmail(String email) {
//     List<Subadmin> list = repo.findByEmail(email);
//     if (list != null && !list.isEmpty()) {
//       return list.get(0);
//     }
//     throw new RuntimeException("Subadmin not found with email: " + email);
//   }

//   /**
//    * Update the details of an existing Subadmin, including file fields.
//    */
//   public Subadmin updateSubAdmin(int id, String name, String lastname, String email, String phoneno,
//       String registercompanyname, String status,
//       MultipartFile stampImg, MultipartFile signature, MultipartFile companylogo, String cinno, String compnayurl,
//       String address) {

//     Subadmin subAdmin = repo.findById(id)
//         .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));

//     subAdmin.setName(name);
//     subAdmin.setLastname(lastname);
//     subAdmin.setEmail(email);
//     subAdmin.setPhoneno(phoneno);
//     subAdmin.setRegistercompanyname(registercompanyname);
//     subAdmin.setStatus(status);
//     subAdmin.setCinno(cinno);
//     subAdmin.setCompanyurl(compnayurl);
//     subAdmin.setAddress(address);

//     if (stampImg != null && !stampImg.isEmpty()) {
//       String stampImgFileName = saveFile(stampImg);
//       subAdmin.setStampImg(stampImgFileName);
//     }
//     if (signature != null && !signature.isEmpty()) {
//       String signatureFileName = saveFile(signature);
//       subAdmin.setSignature(signatureFileName);
//     }
//     if (companylogo != null && !companylogo.isEmpty()) {
//       String logoFileName = saveFile(companylogo);
//       subAdmin.setCompanylogo(logoFileName);
//     }
//     return repo.save(subAdmin);
//   }

//   /**
//    * Update only the image files (stampImg, signature, companylogo) for a
//    * subadmin.
//    */
//   public Subadmin updateSubAdminImages(int id, MultipartFile stampImg, MultipartFile signature,
//       MultipartFile companylogo) {
//     Subadmin subAdmin = repo.findById(id)
//         .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));
//     if (stampImg != null && !stampImg.isEmpty()) {
//       String fileName = saveFile(stampImg);
//       subAdmin.setStampImg(fileName);
//     }
//     if (signature != null && !signature.isEmpty()) {
//       String fileName = saveFile(signature);
//       subAdmin.setSignature(fileName);
//     }
//     if (companylogo != null && !companylogo.isEmpty()) {
//       String fileName = saveFile(companylogo);
//       subAdmin.setCompanylogo(fileName);
//     }
//     return repo.save(subAdmin);
//   }

//   // Login method (plain text password check)
//   public Subadmin login(String email, String password) {
//     List<Subadmin> subAdmins = repo.findByEmail(email);
//     if (!subAdmins.isEmpty()) {
//       Subadmin subAdmin = subAdmins.get(0);
//       if (password.equals(subAdmin.getPassword())) {
//         return subAdmin;
//       }
//     }
//     return null;
//   }

//   // Update Subadmin password.
//   public void updatePassword(int id, String newPassword) {
//     Subadmin subAdmin = repo.findById(id)
//         .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));
//     subAdmin.setPassword(newPassword);
//     repo.save(subAdmin);
//   }

//   // Get all Subadmins.
//   public List<Subadmin> getAllSubAdmins() {
//     return repo.findAll();
//   }

//   // Delete Subadmin by ID.
//   public void deleteSubAdmin(int id) {
//     repo.deleteById(id);
//   }

//   // Utility method for saving a file.
//   private String saveFile(MultipartFile file) {
//     String originalFilename = file.getOriginalFilename();
//     String fileName = System.currentTimeMillis() + "_" + (originalFilename != null ? originalFilename : "file");
//     try {
//       Path uploadPath = Paths.get(uploadDir);
//       // Create the directory if it does not exist.
//       if (!Files.exists(uploadPath)) {
//         Files.createDirectories(uploadPath);
//       }
//       Path filePath = uploadPath.resolve(fileName);
//       Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//     } catch (IOException e) {
//       e.printStackTrace();
//       throw new RuntimeException("File saving failed for: " + fileName, e);
//     }
//     return fileName;
//   }
// }

package com.jaywant.demo.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;

@Service
public class SubAdminService {

  @Autowired
  private SubAdminRepo repo;

  @Autowired
  private EmployeeRepo employeeRepo;

  @Autowired
  private EmployeeDeviceMappingService deviceMappingService;

  private final String uploadDir = "src/main/resources/static/images/profile/";

  public Employee addEmployee(
      int subadminId,
      String firstName,
      String lastName,
      String email,
      Long phone,
      String aadharNo,
      String panCard,
      String education,
      String bloodGroup,
      String jobRole,
      String gender,
      String address,
      String birthDate,
      String joiningDate,
      String status,
      String bankName,
      String bankAccountNo,
      String bankIfscCode,
      String branchName,
      Long salary,
      MultipartFile empimg,
      MultipartFile adharimg,
      MultipartFile panimg,
      String department) {
    Subadmin subadmin = repo.findById(subadminId)
        .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + subadminId));

    Employee e = new Employee();
    e.setFirstName(firstName);
    e.setLastName(lastName);
    e.setEmail(email);
    e.setPhone(phone);
    e.setAadharNo(aadharNo);
    e.setPanCard(panCard);
    e.setEducation(education);
    e.setBloodGroup(bloodGroup);
    e.setJobRole(jobRole);
    e.setGender(gender);
    e.setAddress(address);
    e.setBirthDate(birthDate);
    e.setJoiningDate(joiningDate);
    e.setStatus(status);
    e.setBankName(bankName);
    e.setBankAccountNo(bankAccountNo);
    e.setBankIfscCode(bankIfscCode);
    e.setBranchName(branchName);
    e.setSalary(salary);
    e.setDepartment(department);
    e.setRole("EMPLOYEE");
    e.setSubadmin(subadmin);

    // Map employee to subadmin's device serial number
    e.setDeviceSerialNumber(subadmin.getDeviceSerialNumber());

    e.setPassword(phone.toString());

    // save each image if present
    if (empimg != null && !empimg.isEmpty()) {
      e.setEmpimg(saveFile(empimg));
    }
    if (adharimg != null && !adharimg.isEmpty()) {
      e.setAdharimg(saveFile(adharimg));
    }
    if (panimg != null && !panimg.isEmpty()) {
      e.setPanimg(saveFile(panimg));
    }

    // Save employee first
    Employee savedEmployee = employeeRepo.save(e);

    // Handle automatic device mapping for new employee
    deviceMappingService.handleNewEmployeeDeviceMapping(savedEmployee);

    return savedEmployee;
  }

  public Employee updateEmployee(
      int subadminId,
      int empId,
      String firstName,
      String lastName,
      String email,
      Long phone,
      String aadharNo,
      String panCard,
      String education,
      String bloodGroup,
      String jobRole,
      String gender,
      String address,
      String birthDate,
      String joiningDate,
      String status,
      String bankName,
      String bankAccountNo,
      String bankIfscCode,
      String branchName,
      Long salary,
      String password,
      MultipartFile empimg,
      MultipartFile adharimg,
      MultipartFile panimg,
      String department) {
    Employee e = employeeRepo.findById(empId)
        .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + empId));

    if (e.getSubadmin() == null || e.getSubadmin().getId() != subadminId) {
      throw new RuntimeException("Cannot update employee not under this Subadmin");
    }

    // update all scalar fields
    e.setFirstName(firstName);
    e.setLastName(lastName);
    e.setEmail(email);
    e.setPhone(phone);
    e.setAadharNo(aadharNo);
    e.setPanCard(panCard);
    e.setEducation(education);
    e.setBloodGroup(bloodGroup);
    e.setJobRole(jobRole);
    e.setGender(gender);
    e.setAddress(address);
    e.setBirthDate(birthDate);
    e.setJoiningDate(joiningDate);
    e.setStatus(status);
    e.setBankName(bankName);
    e.setBankAccountNo(bankAccountNo);
    e.setBankIfscCode(bankIfscCode);
    e.setBranchName(branchName);
    e.setSalary(salary);
    e.setDepartment(department);
    // always preserve role
    e.setRole("EMPLOYEE");

    // Update device serial number mapping from subadmin
    if (e.getSubadmin() != null && e.getSubadmin().getDeviceSerialNumber() != null) {
      e.setDeviceSerialNumber(e.getSubadmin().getDeviceSerialNumber());
    }

    e.setPassword(password);

    // update images if provided
    if (empimg != null && !empimg.isEmpty()) {
      e.setEmpimg(saveFile(empimg));
    }
    if (adharimg != null && !adharimg.isEmpty()) {
      e.setAdharimg(saveFile(adharimg));
    }
    if (panimg != null && !panimg.isEmpty()) {
      e.setPanimg(saveFile(panimg));
    }

    // Save employee first
    Employee savedEmployee = employeeRepo.save(e);

    // Update device mapping if subadmin has device serial number
    if (savedEmployee.getSubadmin() != null &&
        savedEmployee.getSubadmin().getDeviceSerialNumber() != null &&
        !savedEmployee.getSubadmin().getDeviceSerialNumber().trim().isEmpty()) {
      deviceMappingService.createEmployeeDeviceMapping(
          savedEmployee, savedEmployee.getSubadmin().getDeviceSerialNumber());
    }

    return savedEmployee;
  }

  // Get Subadmin by email
  public Subadmin getSubAdminByEmail(String email) {
    return repo.findByEmail(email)
        .stream()
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Subadmin not found with email: " + email));
  }

  // Update all fields of subadmin including image files
  public Subadmin updateSubAdmin(
      int id,
      String name,
      String lastname,
      String email,
      String phoneno,
      String registercompanyname,
      String status,
      String cinno,
      String companyurl,
      String address,
      MultipartFile stampImg,
      MultipartFile signature,
      MultipartFile companylogo, Double latitude, Double longitude,
      String packageType, Integer customCount, String emailServerPassword) {

    Subadmin subAdmin = repo.findById(id)
        .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));

    subAdmin.setName(name);
    subAdmin.setLastname(lastname);
    subAdmin.setEmail(email);
    subAdmin.setPhoneno(phoneno);
    subAdmin.setRegistercompanyname(registercompanyname);
    subAdmin.setStatus(status);
    subAdmin.setCinno(cinno);
    subAdmin.setCompanyurl(companyurl);
    subAdmin.setAddress(address);
    subAdmin.setLatitude(latitude);
    subAdmin.setLongitude(longitude);
    subAdmin.setEmailServerPassword(emailServerPassword);

    // Package logic (null-safe)
    if (packageType != null && !packageType.trim().isEmpty()) {
      if ("custom".equalsIgnoreCase(packageType)) {
        subAdmin.setPackageType("custom");
        if (customCount != null) {
          subAdmin.setPackageCount(customCount);
        }
        // If customCount is null, do not update packageCount (leave as is)
      } else {
        subAdmin.setPackageType(packageType);
        switch (packageType) {
          case "10":
            subAdmin.setPackageCount(10);
            break;
          case "15":
            subAdmin.setPackageCount(15);
            break;
          case "30":
            subAdmin.setPackageCount(30);
            break;
          case "40":
            subAdmin.setPackageCount(40);
            break;
          default:
            throw new IllegalArgumentException("Invalid package type");
        }
      }
    }
    // If packageType is null or empty, do not update packageType or packageCount

    // Update optional files when provided
    if (stampImg != null && !stampImg.isEmpty()) {
      subAdmin.setStampImg(saveFile(stampImg));
    }
    if (signature != null && !signature.isEmpty()) {
      subAdmin.setSignature(saveFile(signature));
    }
    if (companylogo != null && !companylogo.isEmpty()) {
      subAdmin.setCompanylogo(saveFile(companylogo));
    }

    return repo.save(subAdmin);
  }

  // Update only images
  public Subadmin updateSubAdminImages(int id, MultipartFile stampImg, MultipartFile signature,
      MultipartFile companylogo) {
    Subadmin subAdmin = repo.findById(id)
        .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));

    if (stampImg != null && !stampImg.isEmpty()) {
      subAdmin.setStampImg(saveFile(stampImg));
    }
    if (signature != null && !signature.isEmpty()) {
      subAdmin.setSignature(saveFile(signature));
    }
    if (companylogo != null && !companylogo.isEmpty()) {
      subAdmin.setCompanylogo(saveFile(companylogo));
    }

    return repo.save(subAdmin);
  }

  // Login with plain-text password
  public Subadmin login(String email, String password) {
    return repo.findByEmail(email)
        .stream()
        .filter(subAdmin -> password.equals(subAdmin.getPassword()))
        .findFirst()
        .orElse(null);
  }

  // Update subadmin password
  public void updatePassword(int id, String newPassword) {
    Subadmin subAdmin = repo.findById(id)
        .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));
    subAdmin.setPassword(newPassword);
    repo.save(subAdmin);
  }

  // Get all subadmins
  public List<Subadmin> getAllSubAdmins() {
    return repo.findAll();
  }

  // Delete subadmin by ID
  public void deleteSubAdmin(int id) {
    if (!repo.existsById(id)) {
      throw new RuntimeException("Subadmin not found with ID: " + id);
    }
    repo.deleteById(id);
  }

  // Save file and return filename
  private String saveFile(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    String fileName = System.currentTimeMillis() + "_" + (originalFilename != null ? originalFilename : "file");

    try {
      Path uploadPath = Paths.get(uploadDir);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      Path filePath = uploadPath.resolve(fileName);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      return fileName;

    } catch (IOException e) {
      throw new RuntimeException("Failed to save file: " + fileName, e);
    }
  }

  public Subadmin updateSubAdmin(int id, String name, String lastname, String email, String phoneno,
      String registercompanyname, String status, String gstno, String cinno, String companyurl, String address,
      MultipartFile stampImg, MultipartFile signature, MultipartFile companylogo, Double latitude, Double longitude,
      String packageType, Integer customCount, String emailServerPassword, String deviceSerialNumber) {

    Subadmin subAdmin = repo.findById(id)
        .orElseThrow(() -> new RuntimeException("Subadmin not found with ID: " + id));

    // Store old device serial number BEFORE updating
    String oldDeviceSerial = subAdmin.getDeviceSerialNumber();

    // Update basic fields
    subAdmin.setName(name);
    subAdmin.setLastname(lastname);
    subAdmin.setEmail(email);
    subAdmin.setPhoneno(phoneno);
    subAdmin.setRegistercompanyname(registercompanyname);
    subAdmin.setStatus(status);
    subAdmin.setGstno(gstno);
    subAdmin.setCinno(cinno);
    subAdmin.setCompanyurl(companyurl);
    subAdmin.setAddress(address);
    subAdmin.setLatitude(latitude);
    subAdmin.setLongitude(longitude);
    subAdmin.setEmailServerPassword(emailServerPassword);
    subAdmin.setDeviceSerialNumber(deviceSerialNumber);

    // Package logic (null-safe)
    if (packageType != null && !packageType.trim().isEmpty()) {
      if ("custom".equalsIgnoreCase(packageType)) {
        subAdmin.setPackageType("custom");
        if (customCount != null) {
          subAdmin.setPackageCount(customCount);
        }
      } else {
        subAdmin.setPackageType(packageType);
        switch (packageType) {
          case "10":
            subAdmin.setPackageCount(10);
            break;
          case "15":
            subAdmin.setPackageCount(15);
            break;
          case "30":
            subAdmin.setPackageCount(30);
            break;
          case "40":
            subAdmin.setPackageCount(40);
            break;
          default:
            throw new IllegalArgumentException("Invalid package type");
        }
      }
    }

    // Update optional files when provided
    if (stampImg != null && !stampImg.isEmpty()) {
      subAdmin.setStampImg(saveFile(stampImg));
    }
    if (signature != null && !signature.isEmpty()) {
      subAdmin.setSignature(saveFile(signature));
    }
    if (companylogo != null && !companylogo.isEmpty()) {
      subAdmin.setCompanylogo(saveFile(companylogo));
    }

    // Save subadmin first
    Subadmin savedSubAdmin = repo.save(subAdmin);

    // Handle device mapping changes
    deviceMappingService.updateSubadminDeviceMapping(
        savedSubAdmin.getId(), oldDeviceSerial, savedSubAdmin.getDeviceSerialNumber());

    return savedSubAdmin;
  }

  // public Subadmin updateSubAdmin(int id, String name, String lastname, String
  // email, String phoneno,
  // String registercompanyname, String status, String cinno, String companyurl,
  // String address,
  // MultipartFile stampImg, MultipartFile signature, MultipartFile companylogo) {
  // // TODO Auto-generated method stub
  // throw new UnsupportedOperationException("Unimplemented method
  // 'updateSubAdmin'");
  // }
}
