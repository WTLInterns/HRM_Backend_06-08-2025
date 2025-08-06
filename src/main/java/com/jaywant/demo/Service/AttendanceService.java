// package com.jaywant.demo.Service;

// import java.util.List;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import com.jaywant.demo.Entity.Attendance;
// import com.jaywant.demo.Entity.Employee;
// import com.jaywant.demo.Entity.Subadmin;
// import com.jaywant.demo.Repo.AttendanceRepo;
// import com.jaywant.demo.Repo.EmployeeRepo;
// import com.jaywant.demo.Repo.SubAdminRepo;

// @Service
// public class AttendanceService {

//   @Autowired
//   private EmployeeRepo employeeRepo;

//   @Autowired
//   private SubAdminRepo subAdminRepo;

//   @Autowired
//   private AttendanceRepo attendanceRepo;

//   public List<Attendance> addAttendance(int subAdminId, String fullName, List<Attendance> attendances) {
//     Employee emp = employeeRepo.findByFullName(fullName);

//     Subadmin subadmin = subAdminRepo.findById(subAdminId).orElse(null);

//     if (emp == null || subadmin == null) {
//       throw new RuntimeException("Employee or Subadmin not found");
//     }

//     // Assign the Employee object to each Attendance record
//     for (Attendance attendance : attendances) {
//       attendance.setEmployee(emp); // 👈 Correct way: pass the whole Employee object
//     }

//     // Save all attendance records
//     return attendanceRepo.saveAll(attendances);
//   }

//   // 🔁 Bulk update attendance
//   public List<Attendance> updateAttendance(int subAdminId, String fullName, List<Attendance> updatedAttendances) {
//     Employee emp = employeeRepo.findByFullName(fullName);
//     Subadmin subadmin = subAdminRepo.findById(subAdminId).orElse(null);

//     if (emp == null || subadmin == null) {
//       throw new RuntimeException("Employee or Subadmin not found");
//     }

//     for (Attendance attendance : updatedAttendances) {
//       // Optional: Add validation to ensure the attendance belongs to this employee
//       attendance.setEmployee(emp); // Rebind employee object
//     }

//     return attendanceRepo.saveAll(updatedAttendances);
//   }

//   public List<Attendance> getAttendanceByBul(String fullName, String date) {
//     return this.attendanceRepo.findByEmployeeFullNameAndDate(fullName, date);
//   }

// }



package com.jaywant.demo.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jaywant.demo.Entity.Attendance;
import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.AttendanceRepo;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.SubAdminRepo;

@Service
public class AttendanceService {

  @Autowired
  private EmployeeRepo employeeRepo;

  @Autowired
  private SubAdminRepo subAdminRepo;

  @Autowired
  private AttendanceRepo attendanceRepo;

  /**
   * Bulk add attendance records.
   */
  public List<Attendance> addAttendance(int subAdminId, String fullName, List<Attendance> attendances) {
    Employee emp = employeeRepo.findByFullName(fullName);
    Subadmin subadmin = subAdminRepo.findById(subAdminId).orElse(null);

    if (emp == null || subadmin == null) {
      throw new RuntimeException("Employee or Subadmin not found");
    }

    // Bind the employee to each attendance record before saving
    for (Attendance attendance : attendances) {
      attendance.setEmployee(emp);
    }
    return attendanceRepo.saveAll(attendances);
  }

  /**
   * Bulk update attendance. For each attendance provided, the method first checks
   * if an attendance record already exists (by employee and date). If found, it
   * updates
   * the status; otherwise, it creates a new record.
   */
  public List<Attendance> updateAttendance(int subAdminId, String fullName, List<Attendance> updatedAttendances) {
    Employee emp = employeeRepo.findByFullName(fullName);
    Subadmin subadmin = subAdminRepo.findById(subAdminId).orElse(null);

    if (emp == null || subadmin == null) {
      throw new RuntimeException("Employee or Subadmin not found");
    }

    List<Attendance> result = new ArrayList<>();

    for (Attendance upd : updatedAttendances) {
      // Look up attendance by employee and date. We assume the date string is in a
      // format that matches what's stored in the DB.
      Optional<Attendance> existingOpt = attendanceRepo.findByEmployeeAndDate(emp, upd.getDate());
      if (existingOpt.isPresent()) {
        Attendance existing = existingOpt.get();
        // Update fields (here we update just the status; add more if needed)
        existing.setStatus(upd.getStatus());
        result.add(attendanceRepo.save(existing));
      } else {
        // Optionally, create a new attendance record if none exists for this date.
        upd.setEmployee(emp);
        result.add(attendanceRepo.save(upd));
      }
    }

    return result;
  }

  public List<Attendance> getAttendanceByBul(int empId, String date) {
    Employee employee = this.employeeRepo.findById(empId)
        .orElseThrow(() -> new RuntimeException("Employee not found with id: " + empId));
    return this.attendanceRepo.findByEmployeeAndDateContaining(employee, date);
  }

  public List<Attendance> getAttendanceForSubAdminByMonth(int subAdminId, String month) {
    List<Employee> employees = employeeRepo.findBySubadminId(subAdminId);
    List<Attendance> monthlyAttendance = new ArrayList<>();
    for (Employee employee : employees) {
        monthlyAttendance.addAll(attendanceRepo.findByEmployeeAndDateStartingWith(employee, month));
    }
    return monthlyAttendance;
}
}
