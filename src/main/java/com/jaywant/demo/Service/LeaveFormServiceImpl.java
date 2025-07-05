package com.jaywant.demo.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jaywant.demo.Entity.Employee;
import com.jaywant.demo.Entity.LeaveForm;
import com.jaywant.demo.Entity.Subadmin;
import com.jaywant.demo.Repo.EmployeeRepo;
import com.jaywant.demo.Repo.LeaveFormRepository;
import com.jaywant.demo.Repo.SubAdminRepo;

@Service
public class LeaveFormServiceImpl implements LeaveFormService {

  @Autowired
  private LeaveFormRepository leaveRepo;

  @Autowired
  private SubAdminRepo subadminRepo;

  @Autowired
  private EmployeeRepo employeeRepo;

  @Autowired
  private NotificationService notificationService;

  private Employee getEmployeeForSubadmin(int subadminId, int empId) {
    Employee employee = employeeRepo.findById(empId)
        .orElseThrow(() -> new RuntimeException("Employee not found with id: " + empId));

    if (employee.getSubadmin() == null || employee.getSubadmin().getId() != subadminId) {
      throw new RuntimeException("Employee does not belong to subadmin");
    }

    return employee;
  }

  @Override
  public LeaveForm createLeaveForm(int subadminId, int empId, LeaveForm leaveForm, String usertoken,
      String subadmintoken) {
    Subadmin subadmin = subadminRepo.findById(subadminId)
        .orElseThrow(() -> new RuntimeException("Subadmin not found"));
    Employee employee = getEmployeeForSubadmin(subadminId, empId);

    leaveForm.setSubadmin(subadmin);
    leaveForm.setEmployee(employee);
    LeaveForm savedLeave = leaveRepo.save(leaveForm);

    if (savedLeave.getEmployee() != null) {
      notificationService.sendLeaveApplicationNotification(subadminId, savedLeave.getEmployee(), savedLeave, usertoken,
          subadmintoken);
    }
    return savedLeave;
  }

  @Override
  public List<LeaveForm> getBySubadminAndEmployeeId(int subadminId, int empId) {
    Employee employee = getEmployeeForSubadmin(subadminId, empId);
    return leaveRepo.findBySubadmin_IdAndEmployee_EmpId(subadminId, employee.getEmpId());
  }

  @Override
  public LeaveForm getLeaveFormById(int subadminId, int empId, int leaveId) {
    LeaveForm leave = leaveRepo.findById(leaveId)
        .orElseThrow(() -> new RuntimeException("LeaveForm not found"));
    // verify it's the right subadmin & employee
    if (leave.getSubadmin().getId() != subadminId ||
        leave.getEmployee().getEmpId() != empId) {
      throw new RuntimeException("Unauthorized access");
    }
    return leave;
  }

  @Override
  public LeaveForm updateLeaveForm(int subadminId, int empId, int leaveId, LeaveForm updatedForm, String usertoken,
      String subadmintoken) {
    LeaveForm existing = getLeaveFormById(subadminId, empId, leaveId);
    existing.setReason(updatedForm.getReason());
    existing.setFromDate(updatedForm.getFromDate());
    existing.setToDate(updatedForm.getToDate());
    existing.setStatus(updatedForm.getStatus());

    LeaveForm savedLeave = leaveRepo.save(existing);

    String newStatus = savedLeave.getStatus();

    notificationService.sendLeaveStatusNotification(
        savedLeave.getEmployee().getEmpId(), savedLeave, newStatus, usertoken, subadmintoken);

    return savedLeave;
  }

  @Override
  public void deleteLeaveForm(int subadminId, int empId, int leaveId) {
    LeaveForm existing = getLeaveFormById(subadminId, empId, leaveId);
    leaveRepo.delete(existing);
  }

  @Override
  public List<LeaveForm> getAllLeavesBySubadmin(int subadminId) {
    // fetch only leaves belonging to the given subadmin
    return leaveRepo.findBySubadmin_Id(subadminId);
  }
}
