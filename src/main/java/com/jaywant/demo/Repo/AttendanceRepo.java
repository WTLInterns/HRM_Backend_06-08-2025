package com.jaywant.demo.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;

@Repository
public interface AttendanceRepo extends JpaRepository<Attendance, Long> {

    List<Attendance> findByEmployee(Employee employee);

    // Optional<Attendance> findByEmployeeIdAndEmployeeNameAndCompanyNameAndDate(
    // Long employeeId, String employeeName, String companyName, LocalDate date);

    Optional<Attendance> findByEmployeeAndDate(Employee employee, String date);

    @Query("SELECT a FROM Attendance a WHERE CONCAT(a.employee.firstName, ' ', a.employee.lastName) = :fullName AND a.date = :date")
    List<Attendance> findByEmployeeFullNameAndDate(@Param("fullName") String fullName, @Param("date") String date);

    List<Attendance> findByEmployeeAndDateContaining(Employee employee, String date);

    List<Attendance> findByDate(String date);

    // Find attendance records within a date range
    @Query("SELECT a FROM Attendance a WHERE a.date >= :fromDate AND a.date <= :toDate ORDER BY a.date DESC")
    List<Attendance> findByDateRange(@Param("fromDate") String fromDate, @Param("toDate") String toDate);

    // Find attendance records for employee within date range
    @Query("SELECT a FROM Attendance a WHERE a.employee = :employee AND a.date >= :fromDate AND a.date <= :toDate ORDER BY a.date DESC")
    List<Attendance> findByEmployeeAndDateRange(@Param("employee") Employee employee,
            @Param("fromDate") String fromDate, @Param("toDate") String toDate);

    // Find attendance records within time range for a specific date
    @Query("SELECT a FROM Attendance a WHERE a.employee = :employee AND a.date = :date AND " +
            "(a.punchInTime BETWEEN :startTime AND :endTime OR a.punchOutTime BETWEEN :startTime AND :endTime)")
    List<Attendance> findByEmployeeAndDateRange(@Param("employee") Employee employee, @Param("date") String date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime);

    // Find attendance by device serial and date
    @Query("SELECT a FROM Attendance a WHERE a.deviceSerial = :deviceSerial AND a.date = :date")
    List<Attendance> findByDeviceSerialAndDate(@Param("deviceSerial") String deviceSerial, @Param("date") String date);

    // Find recent activity by terminal
    @Query("SELECT a FROM Attendance a WHERE a.deviceSerial = :terminalSerial ORDER BY a.date DESC, a.punchOutTime DESC, a.punchInTime DESC")
    List<Attendance> findRecentActivityByTerminal(@Param("terminalSerial") String terminalSerial, int limit);

    // Find incomplete attendance (missing check-out)
    @Query("SELECT a FROM Attendance a WHERE a.date = :date AND a.punchInTime IS NOT NULL AND a.punchOutTime IS NULL")
    List<Attendance> findIncompleteAttendanceByDate(@Param("date") String date);

}
