package com.jaywant.demo.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaywant.demo.DTO.SalaryDTO;
import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Service.AttendanceService;
import com.jaywant.demo.Service.EmployeeEmailService;
import com.jaywant.demo.Service.EmployeePasswordResetService;
import com.jaywant.demo.Service.EmployeeService;
import com.jaywant.demo.Service.EmployeeDeviceMappingService;
import com.jaywant.demo.Service.ImageUploadService;
import com.jaywant.demo.Service.SalaryService;
import com.jaywant.demo.Service.SalarySlipPDFService;
import com.jaywant.demo.Service.SubAdminService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/employee")
// @CrossOrigin(origins = "*")
public class EmployeeController {

  @Value("${file.upload-dir}")
  private String uploadDir;

  @Autowired
  private EmployeeRepo employeeRepository;

  @Autowired
  private AttendanceRepo attendanceRepository;

  @Autowired
  private AttendanceService attendanceService;

  @Autowired
  private EmployeeService empService;

  @Autowired
  private SalaryService salaryService;

  @Autowired
  private SalarySlipPDFService salarySlipPDFService;

  @Autowired
  private SubAdminRepo subAdminRepo;

  @Autowired
  private SubAdminService subAdminService;

  @Autowired
  private EmployeeDeviceMappingService deviceMappingService;

  @Autowired
  private ImageUploadService imageUploadService;

  // =====================================================
  // Attendance Endpoints
  // =====================================================

  /**
   * Fetch all attendance records for an employee by full name and subadmin ID.
   */
  @GetMapping("/{subadminId}/{empId}/attendance")
  public ResponseEntity<?> getAttendanceByEmployeeId(@PathVariable int subadminId,
      @PathVariable int empId) {
    Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (employee == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Employee with ID '" + empId + "' not found for subadmin ID " + subadminId);
    }
    List<Attendance> attendances = attendanceRepository.findByEmployee(employee);
    return ResponseEntity.ok(attendances);
  }

  /**
   * Add a new attendance record for an employee.
   */
  @PostMapping("/{subadminId}/{empId}/attendance/add")
  public ResponseEntity<?> addAttendance(@PathVariable int subadminId,
      @PathVariable int empId,
      @RequestBody Attendance newAttendance) {
    Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (employee == null) {
      return ResponseEntity.badRequest().body("Employee not found for ID: " + empId);
    }

    newAttendance.setEmployee(employee);
    Attendance saved = attendanceRepository.save(newAttendance);
    return ResponseEntity.ok(saved);
  }

  /**
   * Update an existing attendance record.
   */
  @PutMapping("/{subadminId}/{empId}/attendance/update/{attendanceId}")
  public ResponseEntity<?> updateAttendance(@PathVariable int subadminId,
      @PathVariable int empId,
      @PathVariable Long attendanceId,
      @RequestBody Attendance updatedData) {
    Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (employee == null) {
      return ResponseEntity.badRequest().body("Employee not found for ID: " + empId);
    }

    Optional<Attendance> attendanceOpt = attendanceRepository.findById(attendanceId);
    if (attendanceOpt.isEmpty()) {
      return ResponseEntity.badRequest().body("Attendance not found with ID: " + attendanceId);
    }

    Attendance attendance = attendanceOpt.get();
    if (attendance.getEmployee() == null || attendance.getEmployee().getEmpId() != employee.getEmpId()) {
      return ResponseEntity.badRequest().body("Attendance does not belong to the employee with ID: " + empId);
    }

    attendance.setDate(updatedData.getDate());
    attendance.setStatus(updatedData.getStatus());

    Attendance updated = attendanceRepository.save(attendance);
    return ResponseEntity.ok(updated);
  }

  // =====================================================
  // Employee Update / Delete Endpoints
  // =====================================================

  /**
   * Update employee details.
   */

  // @PostMapping("/{subAdminId}/{fullName}/attendance/add")
  // public List<Attendance> addAttendances(@PathVariable int subAdminId,
  // @PathVariable String fullName,
  // @RequestBody List<Attendance> attendances) {
  // return this.attendanceService.addAttendance(subAdminId, fullName,
  // attendances);
  // }

  /**
   * Update Employee by empId (with optional new images).
   */
  @PutMapping(value = "/update-employee/{subadminId}/{empId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> updateEmployeeById(
      @PathVariable int subadminId,
      @PathVariable int empId,

      // scalar fields
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
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String password,

      // image parts
      @RequestPart(required = false) MultipartFile empimg,
      @RequestPart(required = false) MultipartFile adharimg,
      @RequestPart(required = false) MultipartFile panimg) {

    // 1) find the employee by subadmin + empId
    Employee existing = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (existing == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("No employee with ID '" + empId + "' under SubAdmin " + subadminId);
    }

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

    try {
      // 2) delegate to your service using the empId you just found
      Employee updated;
      updated = subAdminService.updateEmployee(
          subadminId,
          empId, // <-- use the real ID
          firstName, lastName, email, phoneNumber,
          aadharNo, panCard, education, bloodGroup,
          jobRole, gender, address,
          birthDate, joiningDate, status,
          bankName, bankAccountNo, bankIfscCode,
          branchName, salary, password,
          empimg, adharimg, panimg, department);
      return ResponseEntity.ok(updated);

    } catch (RuntimeException ex) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body("Error updating employee: " + ex.getMessage());
    }
  }

  /**
   * Delete an employee.
   */
  @DeleteMapping("/{subadminId}/delete/{empId}")
  public ResponseEntity<?> deleteEmployee(@PathVariable int subadminId,
      @PathVariable int empId) {
    try {
      System.out.println("=== DELETE EMPLOYEE REQUEST ===");
      System.out.println("SubAdmin ID: " + subadminId);
      System.out.println("Employee ID: " + empId);

      Optional<Employee> employeeOpt = employeeRepository.findById(empId);
      if (!employeeOpt.isPresent()) {
        System.out.println("Employee not found for ID: " + empId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Employee not found for ID: " + empId);
      }

      Employee employee = employeeOpt.get();
      System.out.println("Found employee: " + employee.getFirstName() + " " + employee.getLastName());

      if (employee.getSubadmin() == null || employee.getSubadmin().getId() != subadminId) {
        System.out.println("Employee does not belong to SubAdmin ID: " + subadminId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body("Employee does not belong to SubAdmin ID: " + subadminId);
      }

      System.out.println("Attempting to delete employee...");

      // Check for device mappings first and remove them if they exist
      System.out.println("Checking for device mappings...");
      try {
        List<EmployeeDeviceMapping> mappings = deviceMappingService.getEmployeeMappings(empId);
        System.out.println("Found " + mappings.size() + " device mappings for employee " + empId);

        if (!mappings.isEmpty()) {
          System.out.println("Removing device mappings...");
          deviceMappingService.removeAllEmployeeMappings(empId);
          System.out.println("Device mappings removed successfully.");
        }
      } catch (Exception mappingException) {
        System.err.println("Error handling device mappings: " + mappingException.getMessage());
        mappingException.printStackTrace();
        throw new RuntimeException("Failed to handle device mappings: " + mappingException.getMessage());
      }

      // Now delete the employee
      System.out.println("Deleting employee from database...");
      try {
        employeeRepository.delete(employee);
        System.out.println("Employee deleted successfully!");
      } catch (Exception deleteException) {
        System.err.println("Error deleting employee: " + deleteException.getMessage());
        deleteException.printStackTrace();
        throw new RuntimeException("Failed to delete employee: " + deleteException.getMessage());
      }

      return ResponseEntity.ok("Employee deleted successfully.");

    } catch (Exception e) {
      System.err.println("=== DELETE EMPLOYEE ERROR ===");
      System.err.println("Error deleting employee ID: " + empId);
      System.err.println("Error message: " + e.getMessage());
      System.err.println("Error class: " + e.getClass().getSimpleName());
      e.printStackTrace();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to delete employee: " + e.getMessage());
    }
  }

  /**
   * Test endpoint to check employee delete prerequisites
   */
  @GetMapping("/{subadminId}/delete-test/{empId}")
  public ResponseEntity<?> testEmployeeDelete(@PathVariable int subadminId, @PathVariable int empId) {
    try {
      Map<String, Object> result = new HashMap<>();

      // Check if employee exists
      Optional<Employee> employeeOpt = employeeRepository.findById(empId);
      result.put("employeeExists", employeeOpt.isPresent());

      if (employeeOpt.isPresent()) {
        Employee employee = employeeOpt.get();
        result.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
        result.put("belongsToSubadmin", employee.getSubadmin() != null && employee.getSubadmin().getId() == subadminId);

        // Check device mappings
        List<EmployeeDeviceMapping> mappings = deviceMappingService.getEmployeeMappings(empId);
        result.put("deviceMappingsCount", mappings.size());
        result.put("deviceMappings", mappings);
      }

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Test failed: " + e.getMessage());
    }
  }

  /**
   * Get full name of employee by ID.
   */
  @GetMapping("/{empId}/fullname")
  public ResponseEntity<?> getEmployeeFullName(@PathVariable int empId) {
    Optional<Employee> employeeOpt = employeeRepository.findById(empId);
    if (!employeeOpt.isPresent()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Employee not found for ID: " + empId);
    }

    Employee employee = employeeOpt.get();
    String fullName = employee.getFirstName() + " " + employee.getLastName();
    return ResponseEntity.ok(fullName);
  }

  /**
   * Get employees by device serial number for biometric device integration.
   */
  @GetMapping("/device/{deviceSerialNumber}")
  public ResponseEntity<?> getEmployeesByDeviceSerialNumber(@PathVariable String deviceSerialNumber) {
    try {
      List<Employee> employees = employeeRepository.findByDeviceSerialNumber(deviceSerialNumber);
      if (employees.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("No employees found for device serial number: " + deviceSerialNumber);
      }
      return ResponseEntity.ok(employees);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error retrieving employees: " + e.getMessage());
    }
  }

  /**
   * GET: Generate salary report for an employee based on attendance dates
   * URL: GET
   * /api/subadmin/employee/{companyName}/{empId}/attendance/report?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd
   */
  @GetMapping("/company/{companyName}/employee/{empId}/attendance/report")
  public ResponseEntity<SalaryDTO> generateSalaryReport(
      @PathVariable String companyName,
      @PathVariable int empId,
      @RequestParam String startDate,
      @RequestParam String endDate) {

    Employee employee = employeeRepository.findById(empId).orElse(null);
    // Check if the employee exists and its associated subadmin's company name
    // matches.
    if (employee == null || employee.getSubadmin() == null ||
        !employee.getSubadmin().getRegistercompanyname().equalsIgnoreCase(companyName)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    String start = startDate.trim();
    String end = endDate.trim();

    SalaryDTO report = salaryService.generateSalaryReport(employee, start, end);
    return ResponseEntity.ok(report);
  }

  /**
   * GET: Generate salary slip for a specific employee under a subadmin
   * URL: GET
   * /api/employee/{subadminId}/{empId}/salary-slip?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd
   */
  @GetMapping("/{subadminId}/{empId}/salary-slip")
  public ResponseEntity<?> getSalarySlip(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestParam String startDate,
      @RequestParam String endDate) {

    try {
      // Verify employee belongs to the subadmin
      Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found or does not belong to this subadmin"));
      }

      String start = startDate.trim();
      String end = endDate.trim();

      SalaryDTO salarySlip = salaryService.generateSalaryReport(employee, start, end);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("employee", Map.of(
          "empId", employee.getEmpId(),
          "fullName", employee.getFullName(),
          "email", employee.getEmail(),
          "department", employee.getDepartment(),
          "jobRole", employee.getJobRole()));
      response.put("salarySlip", salarySlip);
      response.put("period", Map.of("startDate", start, "endDate", end));

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error generating salary slip: " + e.getMessage()));
    }
  }

  /**
   * GET: Get all salary slips for employees under a subadmin
   * URL: GET
   * /api/employee/{subadminId}/salary-slips?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd
   */
  @GetMapping("/{subadminId}/salary-slips")
  public ResponseEntity<?> getAllSalarySlips(
      @PathVariable int subadminId,
      @RequestParam String startDate,
      @RequestParam String endDate) {

    try {
      List<Employee> employees = employeeRepository.findBySubadminId(subadminId);
      if (employees.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "No employees found for this subadmin"));
      }

      String start = startDate.trim();
      String end = endDate.trim();

      List<Map<String, Object>> salarySlips = new ArrayList<>();

      for (Employee employee : employees) {
        try {
          SalaryDTO salarySlip = salaryService.generateSalaryReport(employee, start, end);

          Map<String, Object> employeeSalaryData = new HashMap<>();
          employeeSalaryData.put("empId", employee.getEmpId());
          employeeSalaryData.put("fullName", employee.getFullName());
          employeeSalaryData.put("email", employee.getEmail());
          employeeSalaryData.put("department", employee.getDepartment());
          employeeSalaryData.put("jobRole", employee.getJobRole());
          employeeSalaryData.put("salarySlip", salarySlip);

          salarySlips.add(employeeSalaryData);
        } catch (Exception e) {
          // Log error but continue with other employees
          System.err
              .println("Error generating salary slip for employee " + employee.getEmpId() + ": " + e.getMessage());
        }
      }

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("subadminId", subadminId);
      response.put("period", Map.of("startDate", start, "endDate", end));
      response.put("totalEmployees", employees.size());
      response.put("processedSlips", salarySlips.size());
      response.put("salarySlips", salarySlips);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error generating salary slips: " + e.getMessage()));
    }
  }

  /**
   * GET: Retrieve all employees under a given subadmin.
   * URL: GET /api/employee/{subadminId}/employee/all
   */
  @GetMapping("/{subadminId}/employee/all")
  public ResponseEntity<?> getAllEmployeesForSubadmin(@PathVariable int subadminId) {
    List<Employee> employees = employeeRepository.findBySubadminId(subadminId);
    if (employees == null) {
      return ResponseEntity.ok(new java.util.ArrayList<>());
    }
    return ResponseEntity.ok(employees);
  }

  // @PutMapping("/{subAdminId}/{fullName}/attendance/update/bulk")
  // public ResponseEntity<?> updateBulkAttendance(@PathVariable int subAdminId,
  // @PathVariable String fullName,
  // @RequestBody List<Attendance> attendances) {
  // try {
  // List<Attendance> updated = attendanceService.updateAttendance(subAdminId,
  // fullName, attendances);
  // return ResponseEntity.ok(updated);
  // } catch (RuntimeException e) {
  // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  // }
  // }

  // @PostMapping("/{subAdminId}/{fullName}/attendance/add/bulk")
  // public ResponseEntity<?> addAttendances(@PathVariable int subAdminId,
  // @PathVariable String fullName,
  // @RequestBody List<Attendance> attendances) {
  // try {
  // List<Attendance> saved = attendanceService.addAttendance(subAdminId,
  // fullName, attendances);
  // return ResponseEntity.ok(saved);
  // } catch (RuntimeException e) {
  // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  // }
  // }

  @GetMapping("/bulk/{empId}/{date}")
  public List<Attendance> getAttendancesBullk(@PathVariable int empId, @PathVariable String date) {

    return this.attendanceService.getAttendanceByBul(empId, date);

  }

  // // At top of class:
  // private static final List<String> REQUIRES_REASON = List.of("absent",
  // "paidleave");

  // private boolean requiresReason(String status) {
  // if (status == null) return false;
  // String normalized = status.trim().toLowerCase().replaceAll("[ _]", "");
  // return REQUIRES_REASON.contains(normalized);
  // }

  // /**
  // * Bulk-add or single-add attendance endpoint.
  // * URL: POST /api/employee/{subadminId}/{fullName}/attendance/add/bulk
  // */
  // @PostMapping("/{subadminId}/{fullName}/attendance/add/bulk")
  // public ResponseEntity<?> addOrUpdateAttendances(
  // @PathVariable int subadminId,
  // @PathVariable String fullName,
  // @RequestBody JsonNode payload) {

  // try {
  // ObjectMapper mapper = new ObjectMapper()
  // .registerModule(new JavaTimeModule())
  // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  // List<Attendance> attendances;
  // if (payload.isArray()) {
  // attendances = Arrays.asList(mapper.treeToValue(payload, Attendance[].class));
  // } else {
  // Attendance single = mapper.treeToValue(payload, Attendance.class);
  // attendances = Collections.singletonList(single);
  // }
  // return processBatch(attendances, subadminId, fullName);

  // } catch (JsonProcessingException e) {
  // return ResponseEntity
  // .badRequest()
  // .body("Invalid Attendance JSON payload: " + e.getOriginalMessage());
  // }
  // }

  // /**
  // * Bulk-update or single-update attendance endpoint.
  // * URL: PUT /api/employee/{subadminId}/{fullName}/attendance/update/bulk
  // */
  // @PutMapping("/{subadminId}/{fullName}/attendance/update/bulk")
  // public ResponseEntity<?> updateOrAddAttendances(
  // @PathVariable int subadminId,
  // @PathVariable String fullName,
  // @RequestBody JsonNode payload) {
  // // exactly the same body as addOrUpdate; reuse it:
  // return addOrUpdateAttendances(subadminId, fullName, payload);
  // }

  // /**
  // * Shared logic: for each Attendance, if ID present or record exists for date,
  // * update; otherwise add new.
  // */
  // private ResponseEntity<?> processBatch(
  // List<Attendance> attendances,
  // int subadminId,
  // String fullName) {

  // Employee employee =
  // employeeRepository.findBySubadminIdAndFullName(subadminId, fullName);
  // if (employee == null) {
  // return ResponseEntity.badRequest()
  // .body("Employee not found: " + fullName);
  // }

  // List<Attendance> toPersist = new ArrayList<>();
  // for (Attendance incoming : attendances) {
  // // 1) If status requires reason, enforce it
  // if (requiresReason(incoming.getStatus())
  // && (incoming.getReason() == null || incoming.getReason().isBlank())) {
  // return ResponseEntity.badRequest()
  // .body("Reason required when status='" +
  // incoming.getStatus() +
  // "' for date " + incoming.getDate());
  // }

  // // 2) Find existing by ID or by date
  // Attendance entity;
  // if (incoming.getId() != null) {
  // entity = attendanceRepository.findById(incoming.getId())
  // .orElseThrow(() ->
  // new RuntimeException("Attendance not found: ID " + incoming.getId()));
  // } else {
  // entity = attendanceRepository
  // .findByEmployeeAndDate(employee, incoming.getDate())
  // .orElseGet(() -> {
  // Attendance a = new Attendance();
  // a.setEmployee(employee);
  // return a;
  // });
  // }

  // // 3) Guard against cross-employee tampering
  // if (entity.getId() != null
  // && !Objects.equals(entity.getEmployee().getEmpId(),
  // employee.getEmpId())) {
  // return ResponseEntity.badRequest()
  // .body("Attendance for date " + incoming.getDate()
  // + " does not belong to " + fullName);
  // }

  // // 4) Copy over fields: only overwrite status/reason if provided
  // entity.setDate(incoming.getDate());
  // if (incoming.getStatus() != null) {
  // entity.setStatus(incoming.getStatus());
  // }
  // if (incoming.getReason() != null) {
  // entity.setReason(incoming.getReason());
  // }

  // // 5) Always update times
  // entity.setPunchInTime(incoming.getPunchInTime());
  // entity.setLunchInTime(incoming.getLunchInTime());
  // entity.setLunchOutTime(incoming.getLunchOutTime());
  // entity.setPunchOutTime(incoming.getPunchOutTime());

  // // 6) Recalculate derived fields
  // entity.calculateDurations();

  // toPersist.add(entity);
  // }

  // // 7) Save & return the full list
  // List<Attendance> saved = attendanceRepository.saveAll(toPersist);
  // return ResponseEntity.ok(saved);
  // }

  // at top of class:
  private static final List<String> REQUIRES_REASON = List.of("absent", "paidleave");

  private boolean requiresReason(String status) {
    if (status == null)
      return false;
    String normalized = status.trim().toLowerCase().replaceAll("[ _]", "");
    return REQUIRES_REASON.contains(normalized);
  }

  /**
   * Bulk-add or single-add attendance endpoint.
   * URL: POST /api/employee/{subadminId}/{empId}/attendance/add/bulk
   *
   * Supports both JSON and multipart requests:
   * - JSON: Standard attendance data
   * - Multipart: For image upload when workType = "work from field"
   *
   * Supported image formats: JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF, SVG, ICO,
   * HEIC, HEIF, AVIF, JFIF
   * Maximum file size: 200MB
   *
   * Mobile app compatible - supports ultra-high resolution images from modern
   * mobile cameras and iPhones
   */
  @PostMapping("/{subadminId}/{empId}/attendance/add/bulk")
  public ResponseEntity<?> addOrUpdateAttendances(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestPart(value = "attendance", required = false) String attendanceJson,
      @RequestPart(value = "image", required = false) MultipartFile image,
      @RequestParam(value = "date", required = false) String date,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "workType", required = false) String workType,
      @RequestParam(value = "punchInTime", required = false) String punchInTime,
      @RequestParam(value = "punchOutTime", required = false) String punchOutTime,
      @RequestParam(value = "lunchInTime", required = false) String lunchInTime,
      @RequestParam(value = "lunchOutTime", required = false) String lunchOutTime,
      @RequestParam(value = "reason", required = false) String reason,
      HttpServletRequest request) {

    try {
      ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      List<Attendance> attendances;

      // Check if this is a JSON request or multipart request
      String contentType = request.getContentType();
      boolean isMultipart = contentType != null && contentType.startsWith("multipart/form-data");

      if (isMultipart) {
        // Handle multipart form-data request

        // If attendance JSON is provided, use it
        if (attendanceJson != null && !attendanceJson.trim().isEmpty()) {
          JsonNode attendancePayload = mapper.readTree(attendanceJson);
          if (attendancePayload.isArray()) {
            attendances = Arrays.asList(mapper.treeToValue(attendancePayload, Attendance[].class));
          } else {
            Attendance single = mapper.treeToValue(attendancePayload, Attendance.class);
            attendances = Collections.singletonList(single);
          }
        }
        // Otherwise, create attendance from individual parameters
        else if (date != null) {
          Attendance attendance = new Attendance();
          attendance.setDate(date);
          attendance.setStatus(status != null ? status : "Present");
          attendance.setWorkType(workType != null ? workType : "work from office");

          // Parse time strings to LocalTime objects
          if (punchInTime != null && !punchInTime.trim().isEmpty()) {
            attendance.setPunchInTime(LocalTime.parse(punchInTime));
          }
          if (punchOutTime != null && !punchOutTime.trim().isEmpty()) {
            attendance.setPunchOutTime(LocalTime.parse(punchOutTime));
          }
          if (lunchInTime != null && !lunchInTime.trim().isEmpty()) {
            attendance.setLunchInTime(LocalTime.parse(lunchInTime));
          }
          if (lunchOutTime != null && !lunchOutTime.trim().isEmpty()) {
            attendance.setLunchOutTime(LocalTime.parse(lunchOutTime));
          }

          attendance.setReason(reason != null ? reason : "");
          attendances = Collections.singletonList(attendance);
        } else {
          return ResponseEntity.badRequest().body("Date is required for multipart requests");
        }

        // If image is provided and workType is "work from field", handle image upload
        if (image != null && !image.isEmpty()) {
          return processBatchWithImage(attendances, subadminId, empId, image);
        } else {
          return processBatch(attendances, subadminId, empId);
        }
      } else {
        // Handle JSON request - read from request body
        try {
          String requestBody = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);
          if (requestBody.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("No attendance data provided");
          }

          JsonNode payload = mapper.readTree(requestBody);
          if (payload.isArray()) {
            attendances = Arrays.asList(mapper.treeToValue(payload, Attendance[].class));
          } else {
            Attendance single = mapper.treeToValue(payload, Attendance.class);
            attendances = Collections.singletonList(single);
          }
          return processBatch(attendances, subadminId, empId);
        } catch (Exception e) {
          return ResponseEntity.badRequest().body("Invalid JSON payload: " + e.getMessage());
        }
      }

    } catch (JsonProcessingException e) {
      return ResponseEntity
          .badRequest()
          .body("Invalid Attendance JSON payload: " + e.getOriginalMessage());
    } catch (Exception e) {
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error processing attendance: " + e.getMessage());
    }
  }

  /**
   * Enhanced attendance endpoint with work type and image support
   * URL: POST /api/employee/{subadminId}/{empId}/attendance/add/enhanced
   */
  @PostMapping("/{subadminId}/{empId}/attendance/add/enhanced")
  public ResponseEntity<?> addEnhancedAttendance(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestBody JsonNode payload) {

    try {
      ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      List<Attendance> attendances;
      if (payload.isArray()) {
        attendances = Arrays.asList(mapper.treeToValue(payload, Attendance[].class));
      } else {
        Attendance single = mapper.treeToValue(payload, Attendance.class);
        attendances = Collections.singletonList(single);
      }

      // Enhanced validation for work type and image requirements
      for (Attendance attendance : attendances) {
        if ("work from field".equals(attendance.getWorkType()) &&
            (attendance.getImagePath() == null || attendance.getImagePath().isEmpty())) {
          return ResponseEntity.badRequest()
              .body("Image is required for 'work from field' work type on date: " + attendance.getDate());
        }
      }

      return processBatch(attendances, subadminId, empId);

    } catch (JsonProcessingException e) {
      return ResponseEntity
          .badRequest()
          .body("Invalid Enhanced Attendance JSON payload: " + e.getOriginalMessage());
    }
  }

  /**
   * Bulk-update or single-update attendance endpoint.
   * URL: PUT /api/employee/{subadminId}/{empId}/attendance/update/bulk
   *
   * Supports both JSON and multipart requests:
   * - JSON: Standard attendance data
   * - Multipart: For image upload when workType = "work from field"
   *
   * Supported image formats: JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF, SVG, ICO,
   * HEIC, HEIF, AVIF, JFIF
   * Maximum file size: 200MB
   *
   * Mobile app compatible - supports ultra-high resolution images from modern
   * mobile cameras and iPhones
   * Same functionality as POST endpoint but for updates
   */
  @PutMapping("/{subadminId}/{empId}/attendance/update/bulk")
  public ResponseEntity<?> updateOrAddAttendances(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestPart(value = "attendance", required = false) String attendanceJson,
      @RequestPart(value = "image", required = false) MultipartFile image,
      @RequestParam(value = "date", required = false) String date,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "workType", required = false) String workType,
      @RequestParam(value = "punchInTime", required = false) String punchInTime,
      @RequestParam(value = "punchOutTime", required = false) String punchOutTime,
      @RequestParam(value = "lunchInTime", required = false) String lunchInTime,
      @RequestParam(value = "lunchOutTime", required = false) String lunchOutTime,
      @RequestParam(value = "reason", required = false) String reason,
      HttpServletRequest request) {
    // reuse the same logic as POST
    return addOrUpdateAttendances(subadminId, empId, attendanceJson, image,
        date, status, workType, punchInTime, punchOutTime,
        lunchInTime, lunchOutTime, reason, request);
  }

  /**
   * Shared logic: for each Attendance, if ID present or record exists for date,
   * update; otherwise add new.
   */
  private ResponseEntity<?> processBatch(
      List<Attendance> attendances,
      int subadminId,
      int empId) {

    Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (employee == null) {
      return ResponseEntity.badRequest().body("Employee not found: " + empId);
    }

    List<Attendance> toPersist = new ArrayList<>();
    for (Attendance incoming : attendances) {
      // enforce reason when needed
      if (requiresReason(incoming.getStatus())
          && (incoming.getReason() == null || incoming.getReason().isBlank())) {
        return ResponseEntity.badRequest()
            .body("Reason required when status='" +
                incoming.getStatus() +
                "' for date " + incoming.getDate());
      }

      // find existing or new
      Attendance entity;
      if (incoming.getId() != null) {
        entity = attendanceRepository.findById(incoming.getId())
            .orElseThrow(() -> new RuntimeException("Attendance not found: ID " + incoming.getId()));
      } else {
        entity = attendanceRepository
            .findByEmployeeAndDate(employee, incoming.getDate())
            .orElseGet(() -> {
              Attendance a = new Attendance();
              a.setEmployee(employee);
              return a;
            });
      }

      // guard cross‑employee
      if (entity.getId() != null
          && !Objects.equals(entity.getEmployee().getEmpId(), employee.getEmpId())) {
        return ResponseEntity.badRequest()
            .body("Attendance for date " + incoming.getDate()
                + " does not belong to employee with ID " + empId);
      }

      // copy fields (only overwrite status/reason if provided)
      entity.setDate(incoming.getDate());
      if (incoming.getStatus() != null) {
        entity.setStatus(incoming.getStatus());
      }
      if (incoming.getReason() != null) {
        entity.setReason(incoming.getReason());
      }

      // only overwrite times if they came in the payload
      if (incoming.getPunchInTime() != null) {
        entity.setPunchInTime(incoming.getPunchInTime());
      }
      if (incoming.getLunchInTime() != null) {
        entity.setLunchInTime(incoming.getLunchInTime());
      }
      if (incoming.getLunchOutTime() != null) {
        entity.setLunchOutTime(incoming.getLunchOutTime());
      }
      if (incoming.getPunchOutTime() != null) {
        entity.setPunchOutTime(incoming.getPunchOutTime());
      }

      if (incoming.getPunchOutTime() != null)
        entity.setPunchOutTime(incoming.getPunchOutTime());

      // ✏️ copy workType
      if (incoming.getWorkType() != null) {
        entity.setWorkType(incoming.getWorkType());
      }

      // ✏️ copy imagePath
      if (incoming.getImagePath() != null) {
        entity.setImagePath(incoming.getImagePath());
      }

      // recalc derived fields
      entity.calculateDurations();

      toPersist.add(entity);
    }

    List<Attendance> saved = attendanceRepository.saveAll(toPersist);
    return ResponseEntity.ok(saved);
  }

  /**
   * Process attendance batch with image upload support
   * Enhanced for mobile app compatibility with all image formats
   */
  private ResponseEntity<?> processBatchWithImage(
      List<Attendance> attendances,
      int subadminId,
      int empId,
      MultipartFile image) {

    if (attendances.isEmpty()) {
      return ResponseEntity.badRequest()
          .body("No attendance data provided");
    }

    // For image upload, we expect only one attendance record
    if (attendances.size() > 1) {
      return ResponseEntity.badRequest()
          .body("Image upload supports only single attendance record");
    }

    Attendance incoming = attendances.get(0);

    // Validate work type - only allow image upload for "work from field"
    if (!"work from field".equals(incoming.getWorkType())) {
      return ResponseEntity.badRequest()
          .body("Image upload is only allowed for 'work from field' work type. " +
              ImageUploadService.getSupportedFormats());
    }

    // Validate image file
    if (image == null || image.isEmpty()) {
      return ResponseEntity.badRequest()
          .body("Image file is required for 'work from field' attendance. " +
              ImageUploadService.getSupportedFormats());
    }

    try {
      // Validate employee exists
      Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.badRequest()
            .body("Employee not found for ID: " + empId);
      }

      // Upload image
      String imagePath = imageUploadService.uploadAttendanceImage(image, empId, incoming.getDate());

      // Find existing attendance record for this date
      Optional<Attendance> existingAttendance = attendanceRepository.findByEmployeeAndDate(employee,
          incoming.getDate());

      Attendance attendance;
      if (existingAttendance.isPresent()) {
        // Update existing attendance
        attendance = existingAttendance.get();
        attendance.setImagePath(imagePath);
        attendance.setWorkType(incoming.getWorkType());
        attendance.setStatus(incoming.getStatus() != null ? incoming.getStatus() : "Present");
        if (incoming.getReason() != null) {
          attendance.setReason(incoming.getReason());
        }
      } else {
        // Create new attendance record
        attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(incoming.getDate());
        attendance.setWorkType(incoming.getWorkType());
        attendance.setImagePath(imagePath);
        attendance.setStatus(incoming.getStatus() != null ? incoming.getStatus() : "Present");
        attendance.setReason(incoming.getReason());
      }

      // Copy time fields if provided
      if (incoming.getPunchInTime() != null)
        attendance.setPunchInTime(incoming.getPunchInTime());
      if (incoming.getPunchOutTime() != null)
        attendance.setPunchOutTime(incoming.getPunchOutTime());
      if (incoming.getLunchInTime() != null)
        attendance.setLunchInTime(incoming.getLunchInTime());
      if (incoming.getLunchOutTime() != null)
        attendance.setLunchOutTime(incoming.getLunchOutTime());

      // Recalculate durations
      attendance.calculateDurations();

      Attendance saved = attendanceRepository.save(attendance);

      // Return same format as regular processBatch method (list of attendance
      // records)
      return ResponseEntity.ok(Collections.singletonList(saved));

    } catch (IOException e) {
      // Handle image upload specific errors
      return ResponseEntity.badRequest()
          .body("Image upload failed: " + e.getMessage() + ". " + ImageUploadService.getSupportedFormats());
    } catch (Exception e) {
      // Handle other errors
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to process attendance with image: " + e.getMessage());
    }
  }

  @Autowired
  private EmployeePasswordResetService passwordResetService;

  /**
   * Get all employees for a subadmin (for dropdown/search)
   * GET /api/employee/{subadminId}/list
   */
  @GetMapping("/{subadminId}/list")
  public ResponseEntity<?> getEmployeesList(@PathVariable int subadminId) {
    try {
      List<Employee> employees = employeeRepository.findBySubadminId(subadminId);

      // Return simplified employee data for dropdown
      List<Map<String, Object>> employeeList = new ArrayList<>();
      for (Employee emp : employees) {
        Map<String, Object> empData = new HashMap<>();
        empData.put("empId", emp.getEmpId());
        empData.put("fullName", emp.getFullName());
        empData.put("firstName", emp.getFirstName());
        empData.put("lastName", emp.getLastName());
        employeeList.add(empData);
      }

      return ResponseEntity.ok(employeeList);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error fetching employees: " + e.getMessage());
    }
  }

  /**
   * Get attendance images for specific employee and date
   * GET /api/employee/{subadminId}/{empId}/attendance/images?date=2025-07-03
   */
  @GetMapping("/{subadminId}/{empId}/attendance/images")
  public ResponseEntity<?> getEmployeeAttendanceImages(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestParam String date) {

    try {
      Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.badRequest().body("Employee not found");
      }

      Optional<Attendance> attendance = attendanceRepository.findByEmployeeAndDate(employee, date);

      if (attendance.isPresent() && attendance.get().getImagePath() != null) {
        Attendance att = attendance.get();
        Map<String, Object> result = new HashMap<>();
        result.put("empId", empId);
        result.put("employeeName", employee.getFullName());
        result.put("date", date);
        result.put("workType", att.getWorkType());
        result.put("status", att.getStatus());
        result.put("imagePath", att.getImagePath());
        result.put("imageUrl", "https://api.managifyhr.com/images/" + att.getImagePath().replace("images/", ""));
        result.put("punchInTime", att.getPunchInTime() != null ? att.getPunchInTime().toString() : null);
        result.put("punchOutTime", att.getPunchOutTime() != null ? att.getPunchOutTime().toString() : null);
        return ResponseEntity.ok(result);
      } else {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "No image found for this employee on " + date);
        result.put("empId", empId);
        result.put("employeeName", employee.getFullName());
        result.put("date", date);
        return ResponseEntity.ok(result);
      }
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error fetching attendance images: " + e.getMessage());
    }
  }

  /**
   * Get all attendance images for all employees on specific date
   * GET /api/employee/{subadminId}/attendance/images/all?date=2025-07-03
   */
  @GetMapping("/{subadminId}/attendance/images/all")
  public ResponseEntity<?> getAllAttendanceImages(
      @PathVariable int subadminId,
      @RequestParam String date) {

    try {
      List<Employee> employees = employeeRepository.findBySubadminId(subadminId);
      List<Map<String, Object>> results = new ArrayList<>();

      for (Employee employee : employees) {
        Optional<Attendance> attendance = attendanceRepository.findByEmployeeAndDate(employee, date);

        if (attendance.isPresent() && attendance.get().getImagePath() != null &&
            "work from field".equals(attendance.get().getWorkType())) {
          Attendance att = attendance.get();
          Map<String, Object> result = new HashMap<>();
          result.put("empId", employee.getEmpId());
          result.put("employeeName", employee.getFullName());
          result.put("date", date);
          result.put("workType", att.getWorkType());
          result.put("status", att.getStatus());
          result.put("imagePath", att.getImagePath());
          result.put("imageUrl", "https://api.managifyhr.com/images/" + att.getImagePath().replace("images/", ""));
          result.put("punchInTime", att.getPunchInTime() != null ? att.getPunchInTime().toString() : null);
          result.put("punchOutTime", att.getPunchOutTime() != null ? att.getPunchOutTime().toString() : null);
          results.add(result);
        }
      }

      return ResponseEntity.ok(results);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error fetching all attendance images: " + e.getMessage());
    }
  }

  @Autowired
  private EmployeeEmailService employeeEmailService;

  /**
   * Send an employee their login details via email.
   * POST /api/employee/{subadminId}/send-login-details
   */
  @PostMapping("/{subadminId}/send-login-details")
  public ResponseEntity<String> sendLoginDetails(
      @PathVariable int subadminId,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Integer empId) {

    Employee emp = null;

    if (email != null) {
      emp = employeeRepository.findBySubadminIdAndEmail(subadminId, email);
    } else if (empId != null) {
      emp = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    } else {
      return ResponseEntity.badRequest().body("Please provide either 'email' or 'empId'.");
    }

    if (emp == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("No employee found under SubAdmin " + subadminId);
    }

    try {
      boolean sent = employeeEmailService.sendEmployeeCredentials(subadminId, emp);
      if (!sent) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Failed to send login details to: " + emp.getEmail());
      }
      return ResponseEntity.ok("Login details sent to: " + emp.getEmail());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred: " + e.getMessage());
    }
  }

  /**
   * GET: all attendance records for an employee
   * URL: GET /api/employee/{subadminId}/{fullName}/attendance/all
   */
  @GetMapping("/{subadminId}/{empId}/attendance/all")
  public ResponseEntity<?> getAllAttendance(
      @PathVariable int subadminId,
      @PathVariable int empId) {

    Employee emp = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (emp == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Employee not found: " + empId);
    }
    List<Attendance> list = attendanceRepository.findByEmployee(emp);
    return ResponseEntity.ok(list);
  }

  /**
   * GET: single attendance record for a specific date
   * URL: GET /api/employee/{subadminId}/{fullName}/attendance/{date}
   */
  @GetMapping("/{subadminId}/{empId}/attendance/{date}")
  public ResponseEntity<?> getAttendanceByDate(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @PathVariable String date) {

    Employee emp = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
    if (emp == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("Employee not found: " + empId);
    }
    Optional<Attendance> attOpt = attendanceRepository.findByEmployeeAndDate(emp, date);
    return attOpt
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("No attendance record for date: " + date));
  }

  /**
   * POST /api/employee/login-employee?email=...&password=...
   * Returns the full Employee object (with nested Subadmin) on success.
   */
  @PostMapping("/login-employee")
  public ResponseEntity<?> loginEmployee(
      @RequestParam String email,
      @RequestParam String password) {

    // 1) Find by email
    Optional<Employee> optEmp = empService.findByEmail(email);
    if (optEmp.isEmpty()) {
      return ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body("Employee not found with email: " + email);
    }
    Employee emp = optEmp.get();

    // 2) Check password
    if (!emp.getPassword().equals(password)) {
      return ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body("Incorrect password.");
    }

    // 3) Success! Return the full Employee (includes Subadmin)
    return ResponseEntity.ok(emp);
  }

  @PostMapping(value = "/forgot-password/request", consumes = { MediaType.APPLICATION_JSON_VALUE,
      MediaType.APPLICATION_FORM_URLENCODED_VALUE }, produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> requestForgotPassword(@RequestParam String email) {
    if (email == null || email.isBlank()) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body("Required field: email");
    }

    try {
      passwordResetService.sendResetOTP(email); // Triggers OTP email using Subadmin's email
      return ResponseEntity.ok("OTP sent to email: " + email);
    } catch (RuntimeException ex) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body("Error: " + ex.getMessage());
    }
  }

  // Endpoint for verifying OTP and resetting password
  @PostMapping(value = "/forgot-password/verify", consumes = { MediaType.APPLICATION_JSON_VALUE,
      MediaType.APPLICATION_FORM_URLENCODED_VALUE }, produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> verifyOtpAndResetPassword(
      @RequestParam String email,
      @RequestParam String otp,
      @RequestParam String newPassword) {
    if (email == null || otp == null || newPassword == null) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body("Required fields: email, otp, newPassword");
    }

    if (!passwordResetService.verifyOTP(email, otp)) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body("Invalid or expired OTP.");
    }

    try {
      passwordResetService.resetPassword(email, newPassword);
      return ResponseEntity.ok("Password updated successfully.");
    } catch (RuntimeException ex) {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body("Error: " + ex.getMessage());
    }
  }
  // ==========================================================
  // Location Tracking Endpoints
  // ==========================================================

  /**
   * Get location of a specific employee by subadmin ID and employee ID
   */
  @GetMapping("/{subadminId}/employee/{empId}/location")
  public ResponseEntity<?> getEmployeeLocation(
      @PathVariable int subadminId,
      @PathVariable int empId) {

    try {
      // Verify the employee belongs to this subadmin
      Employee employee = employeeRepository.findById(empId).orElse(null);
      if (employee == null) {
        return ResponseEntity.badRequest().body("Employee not found with ID: " + empId);
      }

      if (employee.getSubadmin() == null || employee.getSubadmin().getId() != subadminId) {
        return ResponseEntity.badRequest().body("Employee does not belong to this subadmin");
      }

      Map<String, Object> locationData = empService.getEmployeeLocation(empId);
      return ResponseEntity.ok(locationData);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Get locations of all employees for a subadmin
   */
  @GetMapping("/{subadminId}/employee/locations")
  public ResponseEntity<?> getAllEmployeeLocations(@PathVariable int subadminId) {
    try {
      List<Map<String, Object>> locations = empService.getAllEmployeeLocations(subadminId);
      return ResponseEntity.ok(locations);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Update an employee's location
   */
  @PutMapping("/{subadminId}/employee/{empId}/location")
  public ResponseEntity<?> updateEmployeeLocation(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestBody Map<String, String> locationData) {

    try {
      // Verify the employee belongs to this subadmin
      Employee employee = employeeRepository.findById(empId).orElse(null);
      if (employee == null) {
        return ResponseEntity.badRequest().body("Employee not found with ID: " + empId);
      }

      if (employee.getSubadmin() == null || employee.getSubadmin().getId() != subadminId) {
        return ResponseEntity.badRequest().body("Employee does not belong to this subadmin");
      }

      String latitude = locationData.get("latitude");
      String longitude = locationData.get("longitude");

      if (latitude == null || longitude == null) {
        return ResponseEntity.badRequest().body("Latitude and longitude are required");
      }

      Employee updatedEmployee = empService.updateLocation(empId, latitude, longitude);
      return ResponseEntity.ok(updatedEmployee);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  @PostMapping("/{subadminId}/employee/{empId}/location")
  public ResponseEntity<?> postEmployeeLocation(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestBody Map<String, String> locationData) {

    return updateEmployeeLocation(subadminId, empId, locationData);
  }

  /**
   * Single‑record add or update endpoint.
   */
  @PostMapping("/{subadminId}/{empId}/attendance/addnew")
  public ResponseEntity<?> addOrUpdateAttendance(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestBody JsonNode payload) throws JsonProcessingException {

    Employee employee = employeeRepository
        .findBySubadminIdAndEmpId(subadminId, empId);
    if (employee == null) {
      return ResponseEntity.badRequest()
          .body("Employee not found: " + empId);
    }

    // 1) Map JSON → Attendance
    Attendance att;
    att = jsonMapper().treeToValue(payload, Attendance.class);

    // 2) Lookup existing or prepare new
    Optional<Attendance> existing = attendanceRepository.findByEmployeeAndDate(employee, att.getDate());
    Attendance entity = existing.orElseGet(() -> {
      Attendance a = new Attendance();
      a.setEmployee(employee);
      return a;
    });

    // 3) Copy only non-null status/reason
    if (att.getStatus() != null) {
      entity.setStatus(att.getStatus());
    }
    if (att.getReason() != null) {
      entity.setReason(att.getReason());
    }

    // 4) Always update the times
    entity.setPunchInTime(att.getPunchInTime());
    entity.setLunchInTime(att.getLunchInTime());
    entity.setLunchOutTime(att.getLunchOutTime());
    entity.setPunchOutTime(att.getPunchOutTime());

    // 5) Recalculate durations
    entity.calculateDurations();

    // 6) Save
    Attendance saved = attendanceRepository.save(entity);
    return ResponseEntity
        .status(existing.isPresent() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(saved);
  }

  private ObjectMapper jsonMapper() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /**
   * POST: Generate and save salary slip PDF for a specific employee
   * URL: POST
   * /api/employee/{subadminId}/{empId}/salary-slip-pdf?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd
   */
  @PostMapping("/{subadminId}/{empId}/salary-slip-pdf")
  public ResponseEntity<?> generateSalarySlipPDF(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestParam String startDate,
      @RequestParam String endDate) {

    try {
      // Verify employee belongs to the subadmin
      Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found or does not belong to this subadmin"));
      }

      String start = startDate.trim();
      String end = endDate.trim();

      // Generate PDF
      String pdfPath = salarySlipPDFService.generateSalarySlipPDF(subadminId, empId, start, end);

      // Get salary data for saving to database
      SalaryDTO salaryData = salaryService.generateSalaryReport(employee, start, end);

      // Save salary slip record to database
      com.jaywant.demo.Entity.SalarySlip salarySlip = salarySlipPDFService.saveSalarySlipRecord(
          employee, employee.getSubadmin(), start, end, pdfPath, salaryData);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Salary slip PDF generated successfully");
      response.put("employee", Map.of(
          "empId", employee.getEmpId(),
          "fullName", employee.getFullName(),
          "email", employee.getEmail(),
          "department", employee.getDepartment(),
          "jobRole", employee.getJobRole()));
      response.put("salarySlip", Map.of(
          "id", salarySlip.getId(),
          "pdfPath", pdfPath,
          "paySlipMonth", salarySlip.getPaySlipMonth(),
          "netPayable", salarySlip.getNetPayable(),
          "grossSalary", salarySlip.getGrossSalary(),
          "createdAt", salarySlip.getCreatedAt()));
      response.put("period", Map.of("startDate", start, "endDate", end));

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error generating salary slip PDF: " + e.getMessage()));
    }
  }

  /**
   * GET: Get all salary slip PDFs for a specific employee
   * URL: GET /api/employee/{subadminId}/{empId}/salary-slip-pdfs
   */
  @GetMapping("/{subadminId}/{empId}/salary-slip-pdfs")
  public ResponseEntity<?> getSalarySlipPDFs(
      @PathVariable int subadminId,
      @PathVariable int empId) {

    try {
      // Verify employee belongs to the subadmin
      Employee employee = employeeRepository.findBySubadminIdAndEmpId(subadminId, empId);
      if (employee == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Employee not found or does not belong to this subadmin"));
      }

      List<com.jaywant.demo.Entity.SalarySlip> salarySlips = salarySlipPDFService.getSalarySlipsByEmployee(employee);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("employee", Map.of(
          "empId", employee.getEmpId(),
          "fullName", employee.getFullName(),
          "email", employee.getEmail(),
          "department", employee.getDepartment(),
          "jobRole", employee.getJobRole()));
      response.put("totalSalarySlips", salarySlips.size());
      response.put("salarySlips", salarySlips.stream().map(slip -> {
        Map<String, Object> slipMap = new HashMap<>();
        slipMap.put("id", slip.getId());
        slipMap.put("pdfPath", slip.getPdfPath());
        slipMap.put("paySlipMonth", slip.getPaySlipMonth());
        slipMap.put("startDate", slip.getStartDate());
        slipMap.put("endDate", slip.getEndDate());
        slipMap.put("netPayable", slip.getNetPayable());
        slipMap.put("grossSalary", slip.getGrossSalary());
        slipMap.put("totalDeductions", slip.getTotalDeductions());
        slipMap.put("workingDays", slip.getWorkingDays());
        slipMap.put("payableDays", slip.getPayableDays());
        slipMap.put("createdAt", slip.getCreatedAt());
        return slipMap;
      }).toList());

      if (salarySlips.isEmpty()) {
        response.put("message", "No salary slip PDFs found for this employee");
      }

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error retrieving salary slip PDFs: " + e.getMessage()));
    }
  }

  /**
   * GET: Serve/Download salary slip file
   * URL: GET /api/employee/salary-slip/file?path={filePath}
   */
  @GetMapping("/salary-slip/file")
  public ResponseEntity<?> serveSalarySlipFile(@RequestParam String path) {
    try {
      // Security check - ensure path is within salary-slips directory
      if (!path.startsWith("/salary-slips/")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied"));
      }

      // Construct full file path
      String fullPath = uploadDir + path;
      File file = new File(fullPath);

      if (!file.exists()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Salary slip file not found"));
      }

      // Read file content
      String content = Files.readString(file.toPath());

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("fileName", file.getName());
      response.put("content", content);
      response.put("filePath", path);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error reading salary slip file: " + e.getMessage()));
    }
  }

  /**
   * GET: Download salary slip file as attachment
   * URL: GET /api/employee/salary-slip/download?path={filePath}
   */
  @GetMapping("/salary-slip/download")
  public ResponseEntity<?> downloadSalarySlipFile(@RequestParam String path) {
    try {
      // Security check - ensure path is within salary-slips directory
      if (!path.startsWith("/salary-slips/")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied"));
      }

      // Construct full file path
      String fullPath = uploadDir + path;
      File file = new File(fullPath);

      if (!file.exists()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "Salary slip file not found"));
      }

      // Prepare file for download
      byte[] fileContent = Files.readAllBytes(file.toPath());

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
          .header(HttpHeaders.CONTENT_TYPE, "text/plain")
          .body(fileContent);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error downloading salary slip file: " + e.getMessage()));
    }
  }
}