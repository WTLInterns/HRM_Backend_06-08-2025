package com.jaywant.demo.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Subadmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String lastname;
    private String stampImg;
    private String signature;
    private String email;
    private String phoneno;
    private String password;
    private String registercompanyname;
    private String companylogo;
    private String roll = "SUB_ADMIN";
    private String gstno;
    private String status;
    private String cinno;
    private String companyurl;
    private String address;

    private Double latitude;
    private Double longitude;
    private String packageType; // 15, 30, 40, custom
    private Integer packageCount; // number of employees allowed

    private String emailServerPassword;
    private String deviceSerialNumber; // Device serial number for biometric mapping

    public String getFcmToken() {

        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
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

    private String fcmToken;
    private Boolean notificationsEnabled = true;
    private LocalDateTime fcmTokenUpdatedAt;

    // getters & setters
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCinno() {
        return cinno;
    }

    public void setCinno(String cinno) {
        this.cinno = cinno;
    }

    public String getCompanyurl() {
        return companyurl;
    }

    public void setCompanyurl(String companyurl) {
        this.companyurl = companyurl;
    }

    @ManyToOne
    @JoinColumn(name = "master_admin_id")
    @JsonBackReference
    private MasterAdmin masterAdmin;

    @OneToMany(mappedBy = "subadmin", cascade = CascadeType.ALL)
    @JsonIgnore // prevents recursion
    private List<Employee> employee;

    @OneToMany(mappedBy = "subadmin", cascade = CascadeType.ALL)
    @JsonIgnore // prevents recursion
    private List<Certificate> certificates;

    @OneToMany(mappedBy = "subadmin", cascade = CascadeType.ALL)
    @JsonIgnore // prevents recursion
    private List<LeaveForm> leaves;

    public List<Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getStampImg() {
        return stampImg;
    }

    public void setStampImg(String stampImg) {
        this.stampImg = stampImg;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneno() {
        return phoneno;
    }

    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRegistercompanyname() {
        return registercompanyname;
    }

    public void setRegistercompanyname(String registercompanyname) {
        this.registercompanyname = registercompanyname;
    }

    public String getCompanylogo() {
        return companylogo;
    }

    public void setCompanylogo(String companylogo) {
        this.companylogo = companylogo;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getGstno() {
        return gstno;
    }

    public void setGstno(String gstno) {
        this.gstno = gstno;
    }

    public MasterAdmin getMasterAdmin() {
        return masterAdmin;
    }

    public void setMasterAdmin(MasterAdmin masterAdmin) {
        this.masterAdmin = masterAdmin;
    }

    public List<Employee> getEmployee() {
        return employee;
    }

    public void setEmployee(List<Employee> employee) {
        this.employee = employee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public Integer getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(Integer packageCount) {
        this.packageCount = packageCount;
    }

    public Subadmin(int id, String name, String lastname, String stampImg, String signature, String email,
            String phoneno, String password, String registercompanyname, String companylogo, String roll, String gstno,
            String status, String cinno, String companyurl, String address, Double latitude, Double longitude,
            MasterAdmin masterAdmin, List<Employee> employee, List<Certificate> certificates, String packageType,
            Integer packageCount, String deviceSerialNumber) {
        this.id = id;
        this.name = name;
        this.lastname = lastname;
        this.stampImg = stampImg;
        this.signature = signature;
        this.email = email;
        this.phoneno = phoneno;
        this.password = password;
        this.registercompanyname = registercompanyname;
        this.companylogo = companylogo;
        this.roll = roll;
        this.gstno = gstno;
        this.status = status;
        this.cinno = cinno;
        this.companyurl = companyurl;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.masterAdmin = masterAdmin;
        this.employee = employee;
        this.certificates = certificates;
        this.packageType = packageType;
        this.packageCount = packageCount;
        this.deviceSerialNumber = deviceSerialNumber;
    }

    public Subadmin() {

    }

    public List<LeaveForm> getLeaves() {
        return leaves;
    }

    public void setLeaves(List<LeaveForm> leaves) {
        this.leaves = leaves;
    }

    public String getEmailServerPassword() {
        return emailServerPassword;
    }

    public void setEmailServerPassword(String emailServerPassword) {
        this.emailServerPassword = emailServerPassword;
    }

    public String getDeviceSerialNumber() {
        return deviceSerialNumber;
    }

    public void setDeviceSerialNumber(String deviceSerialNumber) {
        this.deviceSerialNumber = deviceSerialNumber;
    }

}
