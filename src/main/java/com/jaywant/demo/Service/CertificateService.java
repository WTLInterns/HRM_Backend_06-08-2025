// package com.jaywant.demo.Service;

// import com.jaywant.demo.Entity.Certificate;
// import com.jaywant.demo.Entity.Employee;
// import com.jaywant.demo.Entity.Subadmin;
// import com.jaywant.demo.Repo.CertificateRepo;
// import com.jaywant.demo.Repo.EmployeeRepo;
// import com.jaywant.demo.Repo.SubAdminRepo;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.util.StringUtils;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;
// import java.nio.file.*;
// import java.util.Optional;
// import java.util.UUID;

// @Service
// public class CertificateService {

//   private final String uploadDir = "src/main/resources/static/images/profile/";

//   @Autowired
//   private SubAdminRepo subAdminRepo;

//   @Autowired
//   private EmployeeRepo employeeRepo;

//   @Autowired
//   private CertificateRepo certificateRepo;

//   public Employee getEmployee(int subadminId, String fullName) {
//     Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
//     if (subadminOpt.isEmpty())
//       return null;

//     String[] names = fullName.trim().split(" ");
//     if (names.length < 2)
//       return null;

//     return employeeRepo.findBySubadminAndFirstNameAndLastNameIgnoreCase(subadminOpt.get(), names[0], names[1]);
//   }

//   public String saveFile(MultipartFile file) throws IOException {
//     String fileName = UUID.randomUUID() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
//     Path path = Paths.get(uploadDir + fileName);
//     Files.createDirectories(path.getParent());
//     Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
//     return fileName;
//   }

//   public Certificate uploadCertificate(int subadminId, String employeeFullName, MultipartFile joiningLetter,
//       MultipartFile offerLetter, MultipartFile terminationLetter) throws IOException {
//     Employee employee = getEmployee(subadminId, employeeFullName);
//     if (employee == null)
//       return null;

//     Certificate certificate = new Certificate();
//     certificate.setEmployee(employee);

//     if (joiningLetter != null)
//       certificate.setJoiningLetter(saveFile(joiningLetter));
//     if (offerLetter != null)
//       certificate.setOfferLetter(saveFile(offerLetter));
//     if (terminationLetter != null)
//       certificate.setTerminationLetter(saveFile(terminationLetter));

//     return certificateRepo.save(certificate);
//   }

//   public Optional<Certificate> getCertificates(int subadminId) {
//     return certificateRepo.findById((long) subadminId);
//   }

//   public Certificate updateCertificate(Long certificateId, MultipartFile joiningLetter, MultipartFile offerLetter,
//       MultipartFile terminationLetter) throws IOException {
//     Optional<Certificate> certOpt = certificateRepo.findById(certificateId);
//     if (certOpt.isEmpty())
//       return null;

//     Certificate certificate = certOpt.get();

//     if (joiningLetter != null)
//       certificate.setJoiningLetter(saveFile(joiningLetter));
//     if (offerLetter != null)
//       certificate.setOfferLetter(saveFile(offerLetter));
//     if (terminationLetter != null)
//       certificate.setTerminationLetter(saveFile(terminationLetter));

//     return certificateRepo.save(certificate);
//   }

//   public void deleteCertificate(Long certificateId) {
//     certificateRepo.deleteById(certificateId);
//   }
// }

package com.jaywant.demo.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jaywant.demo.Entity.Certificate;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Exception.ResourceNotFoundException;
import com.jaywant.demo.Repo.CertificateRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;

@Service
public class CertificateService {

  @Value("${file.upload-dir}")
  private String uploadDir;

  private final CertificateRepo certificateRepo;
  private final EmployeeRepo employeeRepo;
  private final SubAdminRepo subAdminRepo;

  @Autowired
  public CertificateService(CertificateRepo certificateRepo, EmployeeRepo employeeRepo, SubAdminRepo subAdminRepo) {
    this.certificateRepo = certificateRepo;
    this.employeeRepo = employeeRepo;
    this.subAdminRepo = subAdminRepo;
  }

  // Create certificate storage directory if it doesn't exist
  private void createDirectoryIfNotExists(String dir) {
    File directory = new File(dir);
    if (!directory.exists()) {
      directory.mkdirs();
    }
  }

  // Generate a unique filename for the uploaded certificate
  private String generateUniqueFileName(String originalFileName) {
    String extension = "";
    if (originalFileName != null && originalFileName.contains(".")) {
      extension = originalFileName.substring(originalFileName.lastIndexOf("."));
    } else {
      extension = ".pdf"; // Default to PDF if no extension provided
    }
    return UUID.randomUUID().toString() + extension;
  }

  // Get employee by subadmin ID and full name
  public Employee getEmployee(int subadminId, int empId) {
    Employee employee = employeeRepo.findById(empId)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + empId));

    if (employee.getSubadmin() == null || employee.getSubadmin().getId() != subadminId) {
      throw new ResourceNotFoundException("Employee with id " + empId + " does not belong to subadmin " + subadminId);
    }
    return employee;
  }

  // Save file to disk
  public String saveFileToDisk(int subadminId, Employee employee, MultipartFile file, String documentType)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
    if (subadminOpt.isEmpty())
      throw new ResourceNotFoundException("Subadmin not found");

    Subadmin subadmin = subadminOpt.get();

    // Create sanitized employee name (replace spaces with underscores)
    String employeeName = (employee.getFirstName() + " " + employee.getLastName()).replaceAll("\\s+", "_");

    // Create directory structure
    String certificateDir = uploadDir + "/certificates/" + subadmin.getId() + "/" + employeeName;
    createDirectoryIfNotExists(certificateDir);

    // Generate unique filename and full path
    String fileName = documentType + "_" + generateUniqueFileName(file.getOriginalFilename());
    Path filePath = Paths.get(certificateDir, fileName);

    // Save the file
    Files.write(filePath, file.getBytes());

    // Return the relative path
    return "/certificates/" + subadmin.getId() + "/" + employeeName + "/" + fileName;
  }

  // Save certificate to database
  public Certificate saveCertificateToDatabase(Employee employee, int subadminId, String documentType,
      String filePath) {
    Optional<Subadmin> subadminOpt = subAdminRepo.findById(subadminId);
    if (subadminOpt.isEmpty())
      throw new ResourceNotFoundException("Subadmin not found");

    Subadmin subadmin = subadminOpt.get();

    // Find existing certificate or create new one
    Optional<Certificate> certificateOpt = certificateRepo.findByEmployeeAndSubadmin(employee, subadmin);
    Certificate certificate = certificateOpt.orElse(new Certificate());

    // Set relationships
    certificate.setEmployee(employee);
    certificate.setSubadmin(subadmin);

    // Set the appropriate path based on document type
    switch (documentType) {
      // Letters
      case "letterHead":
        certificate.setLetterHeadPath(filePath);
        break;
      case "appointment":
        certificate.setAppointmentLetterPath(filePath);
        break;
      case "joining":
        certificate.setJoiningLetterPath(filePath);
        break;
      case "agreement":
        certificate.setAgreementContractPath(filePath);
        break;
      case "increment":
        certificate.setIncrementLetterPath(filePath);
        break;
      case "experience":
        certificate.setExperienceLetterPath(filePath);
        break;
      case "relieving":
        certificate.setRelievingLetterPath(filePath);
        break;
      case "exit":
        certificate.setTerminationLetterPath(filePath);
        break;
      case "termination":
        certificate.setTerminationLetterPath(filePath);
        break;

      // Certificates
      case "internship":
        certificate.setInternshipCompletionPath(filePath);
        break;
      case "achievement":
        certificate.setAchievementCertificatePath(filePath);
        break;
      case "performance":
        certificate.setPerformanceCertificatePath(filePath);
        break;
      case "postAppraisal":
        certificate.setPostAppraisalPath(filePath);
        break;
      default:
        throw new IllegalArgumentException("Unknown document type: " + documentType);
    }

    // Save and return the certificate entry
    return certificateRepo.save(certificate);
  }

  // Get certificates by employee
  public List<Certificate> getCertificatesByEmployee(Employee employee) {
    return certificateRepo.findByEmployee(employee);
  }

  // Get all employees by subadmin ID
  public List<Employee> getEmployeesBySubadmin(int subadminId) {
    return employeeRepo.findBySubadminId(subadminId);
  }

  // Delete a certificate
  public void deleteCertificate(Long certificateId) {
    if (!certificateRepo.existsById(certificateId)) {
      throw new ResourceNotFoundException("Certificate not found with id: " + certificateId);
    }
    certificateRepo.deleteById(certificateId);
  }
}