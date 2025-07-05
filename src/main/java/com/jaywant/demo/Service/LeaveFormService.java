package com.jaywant.demo.Service;

import java.util.List;

import com.jaywant.demo.Entity.LeaveForm;

public interface LeaveFormService {
  LeaveForm createLeaveForm(int subadminId, int empId, LeaveForm leaveForm, String usertoken, String subadmintoken);

  List<LeaveForm> getBySubadminAndEmployeeId(int subadminId, int empId);

  LeaveForm getLeaveFormById(int subadminId, int empId, int leaveId);

  LeaveForm updateLeaveForm(int subadminId, int empId, int leaveId, LeaveForm leaveForm, String usertoken,
      String subadmintoken);

  void deleteLeaveForm(int subadminId, int empId, int leaveId);

  List<LeaveForm> getAllLeavesBySubadmin(int subadminId);
}
