package com.jaywant.demo.Controller;

import java.util.List;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Service.EmployeeService;
import com.jaywant.demo.Entity.LocationHistory;
import com.jaywant.demo.Repo.LocationHistoryRepository;
import com.jaywant.demo.Service.ExcelExportService;

@RestController
@RequestMapping("/api/location")
public class LocationController {

  @Autowired
  private EmployeeService employeeService;

  @Autowired
  private EmployeeRepo employeeRepository;

  @Autowired
  private LocationHistoryRepository locationHistoryRepository;

  @Autowired
  private ExcelExportService excelExportService;

  @Autowired
  private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

  // ==========================================================
  // Location Tracking Endpoints (matches EmployeeController format)
  // ==========================================================

  /**
   * Get location of a specific employee by subadmin ID and employee ID
   */
  @GetMapping("/{subadminId}/employee/{empId}")
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

      Map<String, Object> locationData = employeeService.getEmployeeLocation(empId);
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
      List<Map<String, Object>> locations = employeeService.getAllEmployeeLocations(subadminId);
      return ResponseEntity.ok(locations);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Get locations of only "work from field" employees for a subadmin
   */
  @GetMapping("/{subadminId}/employee/locations/field-work")
  public ResponseEntity<?> getWorkFromFieldEmployeeLocations(@PathVariable int subadminId) {
    try {
      List<Map<String, Object>> locations = employeeService.getWorkFromFieldEmployeeLocations(subadminId);
      return ResponseEntity.ok(locations);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Update an employee's location
   */
  @PutMapping("/{subadminId}/employee/{empId}")
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

      Employee updatedEmployee = employeeService.updateLocation(empId, latitude, longitude);

      // Prepare response for broadcast
      Map<String, Object> response = Map.of(
          "empId", updatedEmployee.getEmpId(),
          "fullName", updatedEmployee.getFullName(),
          "latitude", updatedEmployee.getLatitude(),
          "longitude", updatedEmployee.getLongitude(),
          "lastLatitude", updatedEmployee.getLastLatitude(),
          "lastLongitude", updatedEmployee.getLastLongitude());
      // Broadcast to WebSocket subscribers
      messagingTemplate.convertAndSend("/topic/location/" + subadminId, response);

      return ResponseEntity.ok(updatedEmployee);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Post an employee's location (alternative to PUT for some clients)
   */
  @PostMapping("/{subadminId}/employee/{empId}")
  public ResponseEntity<?> postEmployeeLocation(
      @PathVariable int subadminId,
      @PathVariable int empId,
      @RequestBody Map<String, String> locationData) {

    return updateEmployeeLocation(subadminId, empId, locationData);
  }

  // Original LocationController endpoints with different URL patterns

  /**
   * Alternative endpoint to get location of a specific employee
   */
  @GetMapping("/direct/{empId}")
  public ResponseEntity<?> getEmployeeLocationDirect(@PathVariable int empId) {
    try {
      Map<String, Object> locationData = employeeService.getEmployeeLocation(empId);
      return ResponseEntity.ok(locationData);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Alternative endpoint to get locations of all employees for a subadmin
   */
  @GetMapping("/subadmin/{subadminId}")
  public ResponseEntity<?> getAllEmployeeLocationsDirect(@PathVariable int subadminId) {
    try {
      List<Map<String, Object>> locations = employeeService.getAllEmployeeLocations(subadminId);
      return ResponseEntity.ok(locations);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Alternative endpoint to update an employee's location
   */
  @PutMapping("/direct/{empId}")
  public ResponseEntity<?> updateEmployeeLocationDirect(
      @PathVariable int empId,
      @RequestBody Map<String, String> locationData) {

    try {
      String latitude = locationData.get("latitude");
      String longitude = locationData.get("longitude");

      if (latitude == null || longitude == null) {
        return ResponseEntity.badRequest().body("Latitude and longitude are required");
      }

      Employee updatedEmployee = employeeService.updateLocation(empId, latitude, longitude);
      return ResponseEntity.ok(updatedEmployee);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
  }

  /**
   * Alternative POST endpoint to publish a location update
   */
  @PostMapping("/direct/{empId}")
  public ResponseEntity<?> postEmployeeLocationDirect(
      @PathVariable int empId,
      @RequestBody Map<String, String> locationData) {

    return updateEmployeeLocationDirect(empId, locationData);
  }

  /**
   * WebSocket endpoint to receive and broadcast location updates
   * Client sends to: /app/location/{subadminId}/{empId}
   * Messages are broadcast to: /topic/location/{subadminId}
   */
  @MessageMapping("/location/{subadminId}/{empId}")
  @SendTo("/topic/location/{subadminId}")
  public Map<String, Object> handleLocationUpdate(
      @DestinationVariable int subadminId,
      @DestinationVariable int empId,
      Map<String, String> locationData) {

    try {
      String latitude = locationData.get("latitude");
      String longitude = locationData.get("longitude");

      Employee updatedEmployee = employeeService.updateLocation(empId, latitude, longitude);

      Map<String, Object> response = Map.of(
          "empId", updatedEmployee.getEmpId(),
          "fullName", updatedEmployee.getFullName(),
          "latitude", updatedEmployee.getLatitude(),
          "longitude", updatedEmployee.getLongitude(),
          "lastLatitude", updatedEmployee.getLastLatitude(),
          "lastLongitude", updatedEmployee.getLastLongitude());

      return response;
    } catch (Exception e) {
      return Map.of("error", e.getMessage());
    }
  }

  /**
   * Export last 12 hours of location history for a single employee
   */
  @GetMapping("/export/employee/{empId}")
  public ResponseEntity<InputStreamResource> exportEmployeeLocationHistory(@PathVariable int empId) throws IOException {
    LocalDateTime end = LocalDateTime.now();
    LocalDateTime start = end.minusHours(12);

    List<LocationHistory> locations = locationHistoryRepository
        .findByEmployeeEmpIdAndTimestampBetweenOrderByTimestampAsc(empId, start, end);

    ByteArrayInputStream in = excelExportService.locationsToExcel(locations);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "attachment; filename=employee_" + empId + "_location_history.xlsx");

    return ResponseEntity
        .ok()
        .headers(headers)
        .body(new InputStreamResource(in));
  }

  /**
   * Export last 12 hours of location history for all employees of a subadmin
   */
  @GetMapping("/export/subadmin/{subadminId}")
  public ResponseEntity<InputStreamResource> exportSubadminLocationHistory(@PathVariable int subadminId)
      throws IOException {
    LocalDateTime end = LocalDateTime.now();
    LocalDateTime start = end.minusHours(12);

    List<LocationHistory> locations = locationHistoryRepository.findBySubadminIdAndTimestampBetween(subadminId, start,
        end);

    ByteArrayInputStream in = excelExportService.locationsToExcel(locations);

    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Disposition", "attachment; filename=subadmin_" + subadminId + "_location_history.xlsx");

    return ResponseEntity
        .ok()
        .headers(headers)
        .body(new InputStreamResource(in));
  }
}
