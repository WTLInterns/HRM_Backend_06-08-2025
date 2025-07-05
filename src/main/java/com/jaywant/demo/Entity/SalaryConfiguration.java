package com.jaywant.demo.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "salary_configuration")
public class SalaryConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subadmin_id", nullable = false)
    private Subadmin subadmin;

    // Basic salary percentage (from CTC)
    @Column(name = "basic_percentage", nullable = false)
    private Double basicPercentage = 50.0; // Default 50%

    // Allowances percentages (from basic salary)
    @Column(name = "hra_percentage", nullable = false)
    private Double hraPercentage = 10.0; // Default 10% of basic

    @Column(name = "da_percentage", nullable = false)
    private Double daPercentage = 53.0; // Default 53% of basic

    @Column(name = "special_allowance_percentage", nullable = false)
    private Double specialAllowancePercentage = 37.0; // Default 37% of basic

    // Deductions
    @Column(name = "professional_tax", nullable = false)
    private Double professionalTax = 0.0; // Fixed amount

    @Column(name = "tds_percentage", nullable = false)
    private Double tdsPercentage = 0.0; // Percentage of gross salary

    // Additional allowances (fixed amounts)
    @Column(name = "transport_allowance")
    private Double transportAllowance = 0.0;

    @Column(name = "medical_allowance")
    private Double medicalAllowance = 0.0;

    @Column(name = "food_allowance")
    private Double foodAllowance = 0.0;

    // Additional deductions (fixed amounts)
    @Column(name = "pf_percentage")
    private Double pfPercentage = 0.0; // Provident Fund percentage

    @Column(name = "esi_percentage")
    private Double esiPercentage = 0.0; // ESI percentage

    // Constructors
    public SalaryConfiguration() {
    }

    public SalaryConfiguration(Subadmin subadmin) {
        this.subadmin = subadmin;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subadmin getSubadmin() {
        return subadmin;
    }

    public void setSubadmin(Subadmin subadmin) {
        this.subadmin = subadmin;
    }

    public Double getBasicPercentage() {
        return basicPercentage;
    }

    public void setBasicPercentage(Double basicPercentage) {
        this.basicPercentage = basicPercentage;
    }

    public Double getHraPercentage() {
        return hraPercentage;
    }

    public void setHraPercentage(Double hraPercentage) {
        this.hraPercentage = hraPercentage;
    }

    public Double getDaPercentage() {
        return daPercentage;
    }

    public void setDaPercentage(Double daPercentage) {
        this.daPercentage = daPercentage;
    }

    public Double getSpecialAllowancePercentage() {
        return specialAllowancePercentage;
    }

    public void setSpecialAllowancePercentage(Double specialAllowancePercentage) {
        this.specialAllowancePercentage = specialAllowancePercentage;
    }

    public Double getProfessionalTax() {
        return professionalTax;
    }

    public void setProfessionalTax(Double professionalTax) {
        this.professionalTax = professionalTax;
    }

    public Double getTdsPercentage() {
        return tdsPercentage;
    }

    public void setTdsPercentage(Double tdsPercentage) {
        this.tdsPercentage = tdsPercentage;
    }

    public Double getTransportAllowance() {
        return transportAllowance;
    }

    public void setTransportAllowance(Double transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public Double getMedicalAllowance() {
        return medicalAllowance;
    }

    public void setMedicalAllowance(Double medicalAllowance) {
        this.medicalAllowance = medicalAllowance;
    }

    public Double getFoodAllowance() {
        return foodAllowance;
    }

    public void setFoodAllowance(Double foodAllowance) {
        this.foodAllowance = foodAllowance;
    }

    public Double getPfPercentage() {
        return pfPercentage;
    }

    public void setPfPercentage(Double pfPercentage) {
        this.pfPercentage = pfPercentage;
    }

    public Double getEsiPercentage() {
        return esiPercentage;
    }

    public void setEsiPercentage(Double esiPercentage) {
        this.esiPercentage = esiPercentage;
    }
}
