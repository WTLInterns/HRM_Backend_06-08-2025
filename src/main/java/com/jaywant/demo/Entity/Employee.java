package com.jaywant.demo.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;

@Entity
public class Employee {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int empId;

  private String firstName;
  private String lastName;

  @Column(name = "full_name")
  private String fullName;

  private String email;
  private Long phone;
  private String aadharNo;
  private String panCard;
  private String education;
  private String bloodGroup;
  private String jobRole;
  private String gender;
  private String address;
  private String birthDate;
  private String joiningDate;
  private String status;
  private String bankName;
  private String bankAccountNo;
  private String bankIfscCode;
  private String branchName;
  private Long salary;
  private String empimg;
  private String department;
  private String password;
  private String adharimg;
  private String panimg;
  private String role = "EMPLOYEE";

  private String latitude;
  private String longitude;
  private String lastLatitude;
  private String lastLongitude;

  private String fcmToken;
  private Boolean notificationsEnabled = true;
  private LocalDateTime fcmTokenUpdatedAt;

  public String getFcmToken() {
    return fcmToken;
  }

  public void setFcmToken(String fcmToken) {
    this.fcmToken = fcmToken;
    this.fcmTokenUpdatedAt = LocalDateTime.now();
  }

  public Boolean getNotificationsEnabled() {
    return notificationsEnabled;
  }

  public void setNotificationsEnabled(Boolean notificationsEnabled) {
    this.notificationsEnabled = notificationsEnabled;
  }

  public LocalDateTime getFcmTokenUpdatedAt() {
    return fcmTokenUpdatedAt;
  }

  public void setFcmTokenUpdatedAt(LocalDateTime fcmTokenUpdatedAt) {
    this.fcmTokenUpdatedAt = fcmTokenUpdatedAt;
  }

  @ManyToOne
  @JoinColumn(name = "subadmin_id")
  @JsonIgnoreProperties({ "employee", "leaves", "certificates", "masterAdmin" })
  private Subadmin subadmin;

  @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
  @JsonIgnore
  private List<Attendance> attendance;

  @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonManagedReference("emp-cert")
  private List<Certificate> certificates = new ArrayList<>();

  @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JsonIgnore // prevents recursion
  private List<LeaveForm> leaves;



  public Employee() {
  }

  public Employee(int empId, String firstName, String lastName, String email, Long phone, String aadharNo,
      String panCard, String education, String bloodGroup, String jobRole, String gender, String address,
      String birthDate, String joiningDate, String status, String bankName, String bankAccountNo,
      String bankIfscCode, String branchName, Long salary, String empimg, String department,
      String password, String adharimg, String panimg, String role, Subadmin subadmin,
      List<Attendance> attendance, List<Certificate> certificates) {

    this.empId = empId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phone = phone;
    this.aadharNo = aadharNo;
    this.panCard = panCard;
    this.education = education;
    this.bloodGroup = bloodGroup;
    this.jobRole = jobRole;
    this.gender = gender;
    this.address = address;
    this.birthDate = birthDate;
    this.joiningDate = joiningDate;
    this.status = status;
    this.bankName = bankName;
    this.bankAccountNo = bankAccountNo;
    this.bankIfscCode = bankIfscCode;
    this.branchName = branchName;
    this.salary = salary;
    this.empimg = empimg;
    this.department = department;
    this.password = password;
    this.adharimg = adharimg;
    this.panimg = panimg;
    this.role = role;
    this.subadmin = subadmin;
    this.attendance = attendance;
    this.certificates = certificates;
  }

  @PrePersist
  @PreUpdate
  public void updateFullName() {
    this.fullName = (this.firstName + " " + this.lastName).trim();
  }

  public int getEmpId() {
    return empId;
  }

  public void setEmpId(int empId) {
    this.empId = empId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
    updateFullName();
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
    updateFullName();
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName; // optional override
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Long getPhone() {
    return phone;
  }

  public void setPhone(Long phone) {
    this.phone = phone;
  }

  public String getAadharNo() {
    return aadharNo;
  }

  public void setAadharNo(String aadharNo) {
    this.aadharNo = aadharNo;
  }

  public String getPanCard() {
    return panCard;
  }

  public void setPanCard(String panCard) {
    this.panCard = panCard;
  }

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public String getBloodGroup() {
    return bloodGroup;
  }

  public void setBloodGroup(String bloodGroup) {
    this.bloodGroup = bloodGroup;
  }

  public String getJobRole() {
    return jobRole;
  }

  public void setJobRole(String jobRole) {
    this.jobRole = jobRole;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public String getJoiningDate() {
    return joiningDate;
  }

  public void setJoiningDate(String joiningDate) {
    this.joiningDate = joiningDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getBankAccountNo() {
    return bankAccountNo;
  }

  public void setBankAccountNo(String bankAccountNo) {
    this.bankAccountNo = bankAccountNo;
  }

  public String getBankIfscCode() {
    return bankIfscCode;
  }

  public void setBankIfscCode(String bankIfscCode) {
    this.bankIfscCode = bankIfscCode;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public Long getSalary() {
    return salary;
  }

  public void setSalary(Long salary) {
    this.salary = salary;
  }

  public String getEmpimg() {
    return empimg;
  }

  public void setEmpimg(String empimg) {
    this.empimg = empimg;
  }

  public String getAdharimg() {
    return adharimg;
  }

  public void setAdharimg(String adharimg) {
    this.adharimg = adharimg;
  }

  public String getPanimg() {
    return panimg;
  }

  public void setPanimg(String panimg) {
    this.panimg = panimg;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Subadmin getSubadmin() {
    return subadmin;
  }

  public void setSubadmin(Subadmin subadmin) {
    this.subadmin = subadmin;
  }

  public List<Attendance> getAttendance() {
    return attendance;
  }

  public void setAttendance(List<Attendance> attendance) {
    this.attendance = attendance;
  }

  public List<Certificate> getCertificates() {
    return certificates;
  }

  public void setCertificates(List<Certificate> certificates) {
    this.certificates = certificates;
  }

  public List<LeaveForm> getLeaves() {
    return leaves;
  }

  public void setLeaves(List<LeaveForm> leaves) {
    this.leaves = leaves;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getLatitude() {
    return latitude;
  }

  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  public String getLongitude() {
    return longitude;
  }

  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  public String getLastLatitude() {
    return lastLatitude;
  }

  public void setLastLatitude(String lastLatitude) {
    this.lastLatitude = lastLatitude;
  }

  public String getLastLongitude() {
    return lastLongitude;
  }

  public void setLastLongitude(String lastLongitude) {
    this.lastLongitude = lastLongitude;
  }

}
