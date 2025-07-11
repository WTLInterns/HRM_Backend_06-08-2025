package com.jaywant.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_device_mapping")
public class EmployeeDeviceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hrm_employee_id", nullable = false)
    private Integer hrmEmployeeId;

    @Column(name = "subadmin_id", nullable = false)
    private Integer subadminId;

    @Column(name = "easytime_employee_id", nullable = false)
    private Integer easytimeEmployeeId;

    @Column(name = "terminal_serial", nullable = false, length = 50)
    private String terminalSerial;

    @Column(name = "emp_code", nullable = false, length = 100, unique = true)
    private String empCode;

    @Column(name = "fingerprint_enrolled", nullable = false)
    private Boolean fingerprintEnrolled = false;

    @Column(name = "enrollment_status")
    @Enumerated(EnumType.STRING)
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hrm_employee_id", insertable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subadmin_id", insertable = false, updatable = false)
    private Subadmin subadmin;

    public enum EnrollmentStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    // Constructors
    public EmployeeDeviceMapping() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public EmployeeDeviceMapping(Integer hrmEmployeeId, Integer subadminId,
            Integer easytimeEmployeeId, String terminalSerial, String empCode) {
        this();
        this.hrmEmployeeId = hrmEmployeeId;
        this.subadminId = subadminId;
        this.easytimeEmployeeId = easytimeEmployeeId;
        this.terminalSerial = terminalSerial;
        this.empCode = empCode;
    }

    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getHrmEmployeeId() {
        return hrmEmployeeId;
    }

    public void setHrmEmployeeId(Integer hrmEmployeeId) {
        this.hrmEmployeeId = hrmEmployeeId;
    }

    public Integer getSubadminId() {
        return subadminId;
    }

    public void setSubadminId(Integer subadminId) {
        this.subadminId = subadminId;
    }

    public Integer getEasytimeEmployeeId() {
        return easytimeEmployeeId;
    }

    public void setEasytimeEmployeeId(Integer easytimeEmployeeId) {
        this.easytimeEmployeeId = easytimeEmployeeId;
    }

    public String getTerminalSerial() {
        return terminalSerial;
    }

    public void setTerminalSerial(String terminalSerial) {
        this.terminalSerial = terminalSerial;
    }

    public String getEmpCode() {
        return empCode;
    }

    public void setEmpCode(String empCode) {
        this.empCode = empCode;
    }

    public Boolean getFingerprintEnrolled() {
        return fingerprintEnrolled;
    }

    public void setFingerprintEnrolled(Boolean fingerprintEnrolled) {
        this.fingerprintEnrolled = fingerprintEnrolled;
    }

    public EnrollmentStatus getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(EnrollmentStatus enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    // Utility methods
    public static String generateEmpCode(Integer subadminId, Integer hrmEmployeeId) {
        return String.format("SA%d_EMP%d_%03d", subadminId, hrmEmployeeId,
                System.currentTimeMillis() % 1000);
    }

    // Check if employee is registered for biometric attendance
    public boolean isBiometricEnabled() {
        return this.fingerprintEnrolled && this.enrollmentStatus == EnrollmentStatus.COMPLETED;
    }

    @Override
    public String toString() {
        return "EmployeeDeviceMapping{" +
                "id=" + id +
                ", hrmEmployeeId=" + hrmEmployeeId +
                ", subadminId=" + subadminId +
                ", easytimeEmployeeId=" + easytimeEmployeeId +
                ", terminalSerial='" + terminalSerial + '\'' +
                ", empCode='" + empCode + '\'' +
                ", fingerprintEnrolled=" + fingerprintEnrolled +
                ", enrollmentStatus=" + enrollmentStatus +
                '}';
    }
}
