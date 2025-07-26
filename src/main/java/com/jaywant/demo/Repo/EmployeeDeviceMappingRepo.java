package com.jaywant.demo.Repo;

import com.jaywant.demo.Entity.EmployeeDeviceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDeviceMappingRepo extends JpaRepository<EmployeeDeviceMapping, Long> {

        // Find by HRM employee ID
        List<EmployeeDeviceMapping> findByHrmEmployeeId(Integer hrmEmployeeId);

        // Find by subadmin ID
        List<EmployeeDeviceMapping> findBySubadminId(Integer subadminId);

        // Find by terminal serial
        List<EmployeeDeviceMapping> findByTerminalSerial(String terminalSerial);

        // Find by subadmin and terminal
        List<EmployeeDeviceMapping> findBySubadminIdAndTerminalSerial(Integer subadminId, String terminalSerial);

        // Find by employee code (unique)
        Optional<EmployeeDeviceMapping> findByEmpCode(String empCode);

        // Find by employee code and terminal serial
        Optional<EmployeeDeviceMapping> findByEmpCodeAndTerminalSerial(String empCode, String terminalSerial);

        // Find by EasyTime employee ID and terminal serial (unique combination)
        Optional<EmployeeDeviceMapping> findByEasytimeEmployeeIdAndTerminalSerial(
                        Integer easytimeEmployeeId, String terminalSerial);

        // Find by HRM employee ID and terminal serial
        Optional<EmployeeDeviceMapping> findByHrmEmployeeIdAndTerminalSerial(
                        Integer hrmEmployeeId, String terminalSerial);

        // Check if employee is already mapped to a terminal
        boolean existsByHrmEmployeeIdAndTerminalSerial(Integer hrmEmployeeId, String terminalSerial);

        // Check if EasyTime employee ID is already used on a terminal
        boolean existsByEasytimeEmployeeIdAndTerminalSerial(Integer easytimeEmployeeId, String terminalSerial);

        // Find employees with fingerprint enrolled
        @Query("SELECT edm FROM EmployeeDeviceMapping edm WHERE edm.fingerprintEnrolled = true AND edm.subadminId = :subadminId")
        List<EmployeeDeviceMapping> findEnrolledEmployeesBySubadmin(@Param("subadminId") Integer subadminId);

        // Find employees pending fingerprint enrollment
        @Query("SELECT edm FROM EmployeeDeviceMapping edm WHERE edm.enrollmentStatus = 'PENDING' AND edm.subadminId = :subadminId")
        List<EmployeeDeviceMapping> findPendingEnrollmentsBySubadmin(@Param("subadminId") Integer subadminId);

        // Find next available EasyTime employee ID for a terminal
        @Query("SELECT COALESCE(MAX(edm.easytimeEmployeeId), 0) + 1 FROM EmployeeDeviceMapping edm WHERE edm.terminalSerial = :terminalSerial")
        Integer findNextEasytimeEmployeeId(@Param("terminalSerial") String terminalSerial);

        // Get employee mapping by HRM employee ID and subadmin ID
        @Query("SELECT edm FROM EmployeeDeviceMapping edm WHERE edm.hrmEmployeeId = :hrmEmployeeId AND edm.subadminId = :subadminId")
        List<EmployeeDeviceMapping> findByHrmEmployeeIdAndSubadminId(
                        @Param("hrmEmployeeId") Integer hrmEmployeeId,
                        @Param("subadminId") Integer subadminId);

        // Delete mappings by HRM employee ID
        void deleteByHrmEmployeeId(Integer hrmEmployeeId);

        // Delete mappings by terminal serial
        void deleteByTerminalSerial(String terminalSerial);

        // Count employees enrolled on a terminal
        @Query("SELECT COUNT(edm) FROM EmployeeDeviceMapping edm WHERE edm.terminalSerial = :terminalSerial AND edm.fingerprintEnrolled = true")
        Long countEnrolledEmployeesByTerminal(@Param("terminalSerial") String terminalSerial);

        // Find all mappings for a subadmin with employee details
        @Query("SELECT edm FROM EmployeeDeviceMapping edm WHERE edm.subadminId = :subadminId")
        List<EmployeeDeviceMapping> findBySubadminIdWithEmployeeDetails(@Param("subadminId") Integer subadminId);

        // Enhanced methods for multi-device, multi-subadmin mapping
        Optional<EmployeeDeviceMapping> findByEmpCodeAndTerminalSerialAndSubadminId(String empCode,
                        String terminalSerial, Integer subadminId);

        Optional<EmployeeDeviceMapping> findByEasytimeEmployeeIdAndTerminalSerialAndSubadminId(
                        Integer easytimeEmployeeId, String terminalSerial, Integer subadminId);

        List<EmployeeDeviceMapping> findByTerminalSerialAndSubadminId(String terminalSerial, Integer subadminId);
}
