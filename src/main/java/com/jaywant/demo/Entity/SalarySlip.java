package com.jaywant.demo.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_slips")
public class SalarySlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subadmin_id", nullable = false)
    private Subadmin subadmin;

    @Column(nullable = false)
    private String startDate;

    @Column(nullable = false)
    private String endDate;

    @Column(nullable = false)
    private String paySlipMonth; // e.g., "JUNE 2025"

    @Column(nullable = false)
    private String pdfPath; // Path to the generated PDF

    @Column
    private Double netPayable;

    @Column
    private Double grossSalary;

    @Column
    private Double totalDeductions;

    @Column
    private Integer workingDays;

    @Column
    private Double payableDays;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Constructors
    public SalarySlip() {}

    public SalarySlip(Employee employee, Subadmin subadmin, String startDate, String endDate, 
                     String paySlipMonth, String pdfPath) {
        this.employee = employee;
        this.subadmin = subadmin;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paySlipMonth = paySlipMonth;
        this.pdfPath = pdfPath;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getPaySlipMonth() {
        return paySlipMonth;
    }

    public void setPaySlipMonth(String paySlipMonth) {
        this.paySlipMonth = paySlipMonth;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public Double getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(Double netPayable) {
        this.netPayable = netPayable;
    }

    public Double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(Double grossSalary) {
        this.grossSalary = grossSalary;
    }

    public Double getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(Double totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public Integer getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(Integer workingDays) {
        this.workingDays = workingDays;
    }

    public Double getPayableDays() {
        return payableDays;
    }

    public void setPayableDays(Double payableDays) {
        this.payableDays = payableDays;
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
}
