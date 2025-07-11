package com.jaywant.demo.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EasyTimeProApiService {

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Authenticate with EasyTimePro API and get token
     */
    public String authenticateAndGetToken(String apiUrl, String username, String password) {
        try {
            String authUrl = apiUrl + "/api-token-auth/";

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("username", username);
            authRequest.put("password", password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(authRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(authUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.get("token").asText();
            }

            throw new RuntimeException("Authentication failed: " + response.getStatusCode());

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro authentication error: " + e.getMessage(), e);
        }
    }

    /**
     * Register employee in EasyTimePro system
     */
    public Map<String, Object> registerEmployee(String apiUrl, String token, Map<String, Object> employeeData) {
        try {
            String employeeUrl = apiUrl + "/personnel/api/employees/";

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(employeeData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(employeeUrl, request, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }

            throw new RuntimeException("Employee registration failed: " + response.getStatusCode());

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro employee registration error: " + e.getMessage(), e);
        }
    }

    /**
     * Get all employees from EasyTimePro
     */
    public List<Map<String, Object>> getEmployees(String apiUrl, String token) {
        try {
            String employeeUrl = apiUrl + "/personnel/api/employees/";

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    employeeUrl, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonResponse.get("results"), List.class);
            }

            throw new RuntimeException("Failed to fetch employees: " + response.getStatusCode());

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro employee fetch error: " + e.getMessage(), e);
        }
    }

    /**
     * Get attendance transactions from EasyTimePro
     */
    public List<Map<String, Object>> getAttendanceTransactions(String apiUrl, String token, String fromDate,
            String toDate) {
        try {
            String transactionUrl = apiUrl + "/iclock/api/transactions/";

            // Add date filters if provided
            if (fromDate != null && toDate != null) {
                transactionUrl += "?punch_time__gte=" + fromDate + "&punch_time__lte=" + toDate;
            }

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    transactionUrl, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonResponse.get("results"), List.class);
            }

            throw new RuntimeException("Failed to fetch transactions: " + response.getStatusCode());

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro transaction fetch error: " + e.getMessage(), e);
        }
    }

    /**
     * Get terminal/device information
     */
    public List<Map<String, Object>> getTerminals(String apiUrl, String token) {
        try {
            String terminalUrl = apiUrl + "/iclock/api/terminals/";

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    terminalUrl, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonResponse.get("results"), List.class);
            }

            throw new RuntimeException("Failed to fetch terminals: " + response.getStatusCode());

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro terminal fetch error: " + e.getMessage(), e);
        }
    }

    /**
     * Update employee information in EasyTimePro
     */
    public Map<String, Object> updateEmployee(String apiUrl, String token, Integer employeeId,
            Map<String, Object> updateData) {
        try {
            String updateUrl = apiUrl + "/personnel/api/employees/" + employeeId + "/";

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    updateUrl, HttpMethod.PATCH, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }

            throw new RuntimeException("Employee update failed: " + response.getStatusCode());

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro employee update error: " + e.getMessage(), e);
        }
    }

    /**
     * Delete employee from EasyTimePro
     */
    public boolean deleteEmployee(String apiUrl, String token, Integer employeeId) {
        try {
            String deleteUrl = apiUrl + "/personnel/api/employees/" + employeeId + "/";

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl, HttpMethod.DELETE, request, String.class);

            return response.getStatusCode() == HttpStatus.NO_CONTENT ||
                    response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            throw new RuntimeException("EasyTimePro employee deletion error: " + e.getMessage(), e);
        }
    }

    /**
     * Test API connection
     */
    public boolean testConnection(String apiUrl, String token) {
        try {
            String testUrl = apiUrl + "/personnel/api/employees/?limit=1";

            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    testUrl, HttpMethod.GET, request, String.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create HTTP headers with authorization token
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token " + token);
        return headers;
    }

    /**
     * Create employee data payload for EasyTimePro
     */
    public Map<String, Object> createEmployeePayload(String empCode, String firstName, String lastName,
            Integer employeeId, Integer departmentId, Integer areaId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("emp_code", empCode);
        payload.put("first_name", firstName);
        payload.put("last_name", lastName);
        payload.put("employee_id", employeeId);
        payload.put("department_id", departmentId != null ? departmentId : 1);
        payload.put("area_id", areaId != null ? areaId : 1);
        return payload;
    }
}
