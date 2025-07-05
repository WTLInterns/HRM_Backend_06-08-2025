// package com.jaywant.demo.Entity;

// import com.fasterxml.jackson.annotation.JsonBackReference;

// import jakarta.persistence.*;

// @Entity
// public class Certificate {
//   @Id
//   @GeneratedValue(strategy = GenerationType.AUTO)
//   private Long id;

//   private String joiningLetter;
//   private String offerLetter;
//   private String terminationLetter;

//   @ManyToOne(fetch = FetchType.LAZY)
//   @JoinColumn(name = "employee_id")
//   @JsonBackReference("emp-cert")
//   private Employee employee;

//   // getters & setters
//   public Long getId() {
//     return id;
//   }

//   public void setId(Long id) {
//     this.id = id;
//   }

//   public String getJoiningLetter() {
//     return joiningLetter;
//   }

//   public void setJoiningLetter(String joiningLetter) {
//     this.joiningLetter = joiningLetter;
//   }

//   public String getOfferLetter() {
//     return offerLetter;
//   }

//   public void setOfferLetter(String offerLetter) {
//     this.offerLetter = offerLetter;
//   }

//   public String getTerminationLetter() {
//     return terminationLetter;
//   }

//   public void setTerminationLetter(String terminationLetter) {
//     this.terminationLetter = terminationLetter;
//   }

//   public Employee getEmployee() {
//     return employee;
//   }

//   public void setEmployee(Employee employee) {
//     this.employee = employee;
//   }
// }

package com.jaywant.demo.Entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "certificates")
public class Certificate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Employee relationship
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "employee_id")
  @JsonBackReference("emp-cert")
  private Employee employee;

  // Subadmin relationship
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "subadmin_id")
  @JsonBackReference("subadmin-certificate")
  private Subadmin subadmin;

  // Letter types
  @Column
  private String letterHeadPath;

  @Column
  private String appointmentLetterPath;

  @Column
  private String joiningLetterPath;

  @Column
  private String agreementContractPath;

  @Column
  private String incrementLetterPath;

  @Column
  private String experienceLetterPath;

  @Column
  private String relievingLetterPath;

  @Column
  private String exitLetterPath;

  @Column
  private String terminationLetterPath;

  // Certificate types
  @Column
  private String internshipCompletionPath;

  @Column
  private String achievementCertificatePath;

  @Column
  private String performanceCertificatePath;

  @Column
  private String postAppraisalPath;

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

  public String getLetterHeadPath() {
    return letterHeadPath;
  }

  public void setLetterHeadPath(String letterHeadPath) {
    this.letterHeadPath = letterHeadPath;
  }

  public String getAppointmentLetterPath() {
    return appointmentLetterPath;
  }

  public void setAppointmentLetterPath(String appointmentLetterPath) {
    this.appointmentLetterPath = appointmentLetterPath;
  }

  public String getJoiningLetterPath() {
    return joiningLetterPath;
  }

  public void setJoiningLetterPath(String joiningLetterPath) {
    this.joiningLetterPath = joiningLetterPath;
  }

  public String getAgreementContractPath() {
    return agreementContractPath;
  }

  public void setAgreementContractPath(String agreementContractPath) {
    this.agreementContractPath = agreementContractPath;
  }

  public String getIncrementLetterPath() {
    return incrementLetterPath;
  }

  public void setIncrementLetterPath(String incrementLetterPath) {
    this.incrementLetterPath = incrementLetterPath;
  }

  public String getExperienceLetterPath() {
    return experienceLetterPath;
  }

  public void setExperienceLetterPath(String experienceLetterPath) {
    this.experienceLetterPath = experienceLetterPath;
  }

  public String getRelievingLetterPath() {
    return relievingLetterPath;
  }

  public void setRelievingLetterPath(String relievingLetterPath) {
    this.relievingLetterPath = relievingLetterPath;
  }

  public String getExitLetterPath() {
    return exitLetterPath;
  }

  public void setExitLetterPath(String exitLetterPath) {
    this.exitLetterPath = exitLetterPath;
  }

  public String getTerminationLetterPath() {
    return terminationLetterPath;
  }

  public void setTerminationLetterPath(String terminationLetterPath) {
    this.terminationLetterPath = terminationLetterPath;
  }

  public String getInternshipCompletionPath() {
    return internshipCompletionPath;
  }

  public void setInternshipCompletionPath(String internshipCompletionPath) {
    this.internshipCompletionPath = internshipCompletionPath;
  }

  public String getAchievementCertificatePath() {
    return achievementCertificatePath;
  }

  public void setAchievementCertificatePath(String achievementCertificatePath) {
    this.achievementCertificatePath = achievementCertificatePath;
  }

  public String getPerformanceCertificatePath() {
    return performanceCertificatePath;
  }

  public void setPerformanceCertificatePath(String performanceCertificatePath) {
    this.performanceCertificatePath = performanceCertificatePath;
  }

  public String getPostAppraisalPath() {
    return postAppraisalPath;
  }

  public void setPostAppraisalPath(String postAppraisalPath) {
    this.postAppraisalPath = postAppraisalPath;
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