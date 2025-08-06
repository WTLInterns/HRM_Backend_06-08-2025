package com.jaywant.demo.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;
import com.jaywant.demo.Entity.LocationHistory;
import com.jaywant.demo.Repo.LocationHistoryRepository;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmployeeService {

  @Value("${google.maps.api.key:}")
  private String apiKey;

  @Autowired
  private EmployeeRepo employeeRepo;

  @Autowired
  private SubAdminRepo subAdminRepo;

  @Autowired
  private AttendanceRepo attendanceRepo;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private LocationHistoryRepository locationHistoryRepository;

  @Autowired
  private RestTemplate restTemplate;

  public Optional<Employee> findByEmail(String email) {
    return employeeRepo.findByEmail(email);
  }

  public Employee findBySubadminIdAndFullName(int subadminId, String employeeFullName) {
    return employeeRepo.findBySubadminIdAndFullName(subadminId, employeeFullName);
  }

  public List<Attendance> addAttendance(int subAdminId, String fullName, List<Attendance> attendances) {
    Employee emp = employeeRepo.findByFullName(fullName);
    Subadmin subadmin = subAdminRepo.findById(subAdminId).orElse(null);

    if (emp == null || subadmin == null) {
      throw new RuntimeException("Employee or Subadmin not found");
    }

    for (Attendance attendance : attendances) {
      attendance.setEmployee(emp);
    }

    return attendanceRepo.saveAll(attendances);
  }

  public void updatePassword(int empId, String newPassword) {
    Optional<Employee> opt = employeeRepo.findById(empId);
    if (!opt.isPresent()) {
      throw new RuntimeException("Employee not found with id: " + empId);
    }
    Employee emp = opt.get();
    // direct assignment, no encoding
    emp.setPassword(newPassword);
    employeeRepo.save(emp);
  }

  /**
   * Update an employee's current location
   */
  public Employee updateLocation(int empId, String latitude, String longitude) {
    Optional<Employee> opt = employeeRepo.findById(empId);
    if (!opt.isPresent()) {
      throw new RuntimeException("Employee not found with id: " + empId);
    }

    Employee emp = opt.get();

    // Store the previous location as last location
    if (emp.getLatitude() != null && emp.getLongitude() != null) {
      emp.setLastLatitude(emp.getLatitude());
      emp.setLastLongitude(emp.getLongitude());
    }

    // Update current location
    emp.setLatitude(latitude);
    emp.setLongitude(longitude);

    Employee updatedEmployee = employeeRepo.save(emp);

    // Fetch address and save to location history
    String address = reverseGeocode(latitude, longitude);
    LocationHistory history = new LocationHistory(updatedEmployee, latitude, longitude, address, LocalDateTime.now());
    locationHistoryRepository.save(history);

    // Send the updated location via WebSocket to subscribers
    if (updatedEmployee.getSubadmin() != null) {
      String destination = "/topic/location/" + updatedEmployee.getSubadmin().getId();
      Map<String, Object> locationData = new HashMap<>();
      locationData.put("empId", updatedEmployee.getEmpId());
      locationData.put("fullName", updatedEmployee.getFullName());
      locationData.put("latitude", updatedEmployee.getLatitude());
      locationData.put("longitude", updatedEmployee.getLongitude());
      locationData.put("lastLatitude", updatedEmployee.getLastLatitude());
      locationData.put("lastLongitude", updatedEmployee.getLastLongitude());

      messagingTemplate.convertAndSend(destination, locationData);
    }

    return updatedEmployee;
  }

  private String reverseGeocode(String lat, String lon) {
    try {
      // Check if API key is available
      if (apiKey == null || apiKey.trim().isEmpty()) {
        System.out.println("Google Maps API key not configured, skipping reverse geocoding");
        return "Location: " + lat + ", " + lon;
      }

      String url = String.format(
          "https://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&key=%s",
          lat, lon, apiKey);
      String response = restTemplate.getForObject(url, String.class);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(response);

      if (root.path("status").asText().equals("OK")) {
        JsonNode results = root.path("results");
        if (results.isArray() && results.size() > 0) {
          // Return the first, most specific address
          return results.get(0).path("formatted_address").asText("Address not found");
        }
      }
      return "N/A";
    } catch (Exception e) {
      // Log the error for debugging, but don't crash the location update
      System.err.println("Reverse geocoding failed: " + e.getMessage());
      return "Address lookup failed";
    }
  }

  /**
   * Get an employee's current location
   */
  public Map<String, Object> getEmployeeLocation(int empId) {
    Optional<Employee> opt = employeeRepo.findById(empId);
    if (!opt.isPresent()) {
      throw new RuntimeException("Employee not found with id: " + empId);
    }

    Employee emp = opt.get();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put("empId", emp.getEmpId());
    locationData.put("fullName", emp.getFullName());
    locationData.put("latitude", emp.getLatitude());
    locationData.put("longitude", emp.getLongitude());
    locationData.put("lastLatitude", emp.getLastLatitude());
    locationData.put("lastLongitude", emp.getLastLongitude());
    locationData.put("apiKey", apiKey);

    return locationData;
  }

  /**
   * Get all employee locations for a specific subadmin
   */
  public List<Map<String, Object>> getAllEmployeeLocations(int subadminId) {
    // Correctly filter employees by the provided subadmin ID
    List<Employee> employees = employeeRepo.findBySubadminId(subadminId);
    List<Map<String, Object>> locations = new java.util.ArrayList<>();

    for (Employee emp : employees) {
      // Ensure employee belongs to the subadmin before adding
      if (emp.getSubadmin() != null && emp.getSubadmin().getId() == subadminId && emp.getLatitude() != null
          && emp.getLongitude() != null) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("empId", emp.getEmpId());
        locationData.put("fullName", emp.getFullName());
        locationData.put("latitude", emp.getLatitude());
        locationData.put("longitude", emp.getLongitude());
        locationData.put("lastLatitude", emp.getLastLatitude());
        locationData.put("lastLongitude", emp.getLastLongitude());
        locations.add(locationData);
      }
    }

    return locations;
  }

  /**
   * Get locations of only "work from field" employees for a specific subadmin
   */
  public List<Map<String, Object>> getWorkFromFieldEmployeeLocations(int subadminId) {
    // Get today's date for checking current attendance
    String today = java.time.LocalDate.now().toString();

    // Get all employees for the subadmin
    List<Employee> employees = employeeRepo.findBySubadminId(subadminId);
    List<Map<String, Object>> locations = new java.util.ArrayList<>();

    for (Employee emp : employees) {
      // Ensure employee belongs to the subadmin and has location data
      if (emp.getSubadmin() != null && emp.getSubadmin().getId() == subadminId &&
          emp.getLatitude() != null && emp.getLongitude() != null) {

        // Check if employee has "work from field" attendance for today
        boolean isWorkFromField = emp.getAttendances().stream()
            .anyMatch(attendance -> today.equals(attendance.getDate()) &&
                "work from field".equals(attendance.getWorkType()));

        // Only add to locations if employee is working from field today
        if (isWorkFromField) {
          Map<String, Object> locationData = new HashMap<>();
          locationData.put("empId", emp.getEmpId());
          locationData.put("fullName", emp.getFullName());
          locationData.put("latitude", emp.getLatitude());
          locationData.put("longitude", emp.getLongitude());
          locationData.put("lastLatitude", emp.getLastLatitude());
          locationData.put("lastLongitude", emp.getLastLongitude());
          locationData.put("workType", "work from field"); // Add work type for clarity
          locations.add(locationData);
        }
      }
    }

    return locations;
  }
}