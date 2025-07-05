package com.jaywant.demo.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "leaveform")
public class LeaveForm {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int leaveId;

  @Column
  private String reason;

  @Column
  private String fromDate;

  @Column
  private String toDate;

  @Column
  private String status;

  @ManyToOne
  @JoinColumn(name = "employee_id")
  private Employee employee;

  @ManyToOne
  @JoinColumn(name = "subadmin_id")
  private Subadmin subadmin;

  public LeaveForm() {
  }

  // Getters and Setters
  public int getLeaveId() {
    return leaveId;
  }

  public void setLeaveId(int leaveId) {
    this.leaveId = leaveId;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  public Subadmin getSubadmin() {
    return subadmin;
  }

  public void setSubadmin(Subadmin subadmin) {
    this.subadmin = subadmin;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getFromDate() {
    return fromDate;
  }

  public void setFromDate(String fromDate) {
    this.fromDate = fromDate;
  }

  public String getToDate() {
    return toDate;
  }

  public void setToDate(String toDate) {
    this.toDate = toDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
