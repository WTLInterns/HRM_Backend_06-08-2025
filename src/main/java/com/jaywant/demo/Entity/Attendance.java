// package com.jaywant.demo.Entity;

// import java.time.Duration;
// import java.time.LocalTime;
// import java.time.temporal.ChronoUnit;

// import com.fasterxml.jackson.annotation.JsonBackReference;
// import com.fasterxml.jackson.annotation.JsonFormat;

// import jakarta.persistence.*;

// @Entity
// public class Attendance {

//   @Id
//   @GeneratedValue(strategy = GenerationType.IDENTITY)
//   private Long id;

//   private String date;
//   private String status;

//   @Column(length = 500)
//   private String reason;

//   private String workingHours;
//   private String breakDuration;

//   @Column(columnDefinition = "TIME(0)")
//   private LocalTime punchInTime;

//   @Column(columnDefinition = "TIME(0)")
//   private LocalTime punchOutTime;

//   @Column(columnDefinition = "TIME(0)")
//   private LocalTime lunchInTime;

//   @Column(columnDefinition = "TIME(0)")
//   private LocalTime lunchOutTime;

//   @ManyToOne
//   @JoinColumn(name = "employee_id")
//   @JsonBackReference
//   private Employee employee;

//   // Automatically calculate durations before save/update
//   @PrePersist
//   @PreUpdate
//   private void preSave() {
//     this.calculateDurations();
//   }

//   // ==== Getters and Setters ====

//   public Long getId() {
//     return id;
//   }

//   public void setId(Long id) {
//     this.id = id;
//   }

//   public String getDate() {
//     return date;
//   }

//   public void setDate(String date) {
//     this.date = date;
//   }

//   public String getStatus() {
//     return status;
//   }

//   public void setStatus(String status) {
//     this.status = status;
//   }

//   public String getReason() {
//     return reason;
//   }

//   public void setReason(String reason) {
//     this.reason = reason;
//   }

//   @JsonFormat(pattern = "HH:mm")
//   public LocalTime getPunchInTime() {
//     return punchInTime;
//   }

//   public void setPunchInTime(LocalTime punchInTime) {
//     this.punchInTime = punchInTime != null ? punchInTime.truncatedTo(ChronoUnit.MINUTES) : null;
//   }

//   @JsonFormat(pattern = "HH:mm")
//   public LocalTime getPunchOutTime() {
//     return punchOutTime;
//   }

//   public void setPunchOutTime(LocalTime punchOutTime) {
//     this.punchOutTime = punchOutTime != null ? punchOutTime.truncatedTo(ChronoUnit.MINUTES) : null;
//   }

//   @JsonFormat(pattern = "HH:mm")
//   public LocalTime getLunchInTime() {
//     return lunchInTime;
//   }

//   public void setLunchInTime(LocalTime lunchInTime) {
//     this.lunchInTime = lunchInTime != null ? lunchInTime.truncatedTo(ChronoUnit.MINUTES) : null;
//   }

//   @JsonFormat(pattern = "HH:mm")
//   public LocalTime getLunchOutTime() {
//     return lunchOutTime;
//   }

//   public void setLunchOutTime(LocalTime lunchOutTime) {
//     this.lunchOutTime = lunchOutTime != null ? lunchOutTime.truncatedTo(ChronoUnit.MINUTES) : null;
//   }

//   public Employee getEmployee() {
//     return employee;
//   }

//   public void setEmployee(Employee employee) {
//     this.employee = employee;
//   }

//   public String getWorkingHours() {
//     return workingHours;
//   }

//   public void setWorkingHours(String workingHours) {
//     this.workingHours = workingHours;
//   }

//   public String getBreakDuration() {
//     return breakDuration;
//   }

//   public void setBreakDuration(String breakDuration) {
//     this.breakDuration = breakDuration;
//   }

//   // ==== Calculate Working and Break Durations ====

//   public void calculateDurations() {
//     if (punchInTime != null && punchOutTime != null) {
//       LocalTime in = punchInTime.truncatedTo(ChronoUnit.MINUTES);
//       LocalTime out = punchOutTime.truncatedTo(ChronoUnit.MINUTES);

//       Duration total = Duration.between(in, out);
//       if (total.isNegative())
//         total = Duration.ZERO;

//       Duration lunch = Duration.ZERO;
//       if (lunchInTime != null && lunchOutTime != null) {
//         LocalTime lin = lunchInTime.truncatedTo(ChronoUnit.MINUTES);
//         LocalTime lout = lunchOutTime.truncatedTo(ChronoUnit.MINUTES);
//         lunch = Duration.between(lin, lout);
//         if (lunch.isNegative())
//           lunch = Duration.ZERO;

//         this.breakDuration = lunch.toHours() + "h " + lunch.toMinutesPart() + "m";
//       } else {
//         this.breakDuration = "0h 0m";
//       }

//       Duration actual = total.minus(lunch);
//       if (actual.isNegative())
//         actual = Duration.ZERO;

//       this.workingHours = actual.toHours() + "h " + actual.toMinutesPart() + "m";
//     } else {
//       this.workingHours = "0h 0m";
//       this.breakDuration = "0h 0m";
//     }
//   }
// }

package com.jaywant.demo.Entity;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
public class Attendance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String date;
  private String status;

  @Column(length = 500)
  private String reason;

  private String workingHours;
  private String breakDuration;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  @Column(columnDefinition = "TIME(0)")
  private LocalTime punchInTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  @Column(columnDefinition = "TIME(0)")
  private LocalTime punchOutTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  @Column(columnDefinition = "TIME(0)")
  private LocalTime lunchInTime;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
  @Column(columnDefinition = "TIME(0)")
  private LocalTime lunchOutTime;

  // Additional fields to match database table structure
  private String lunchPunchIn; // lunch_punch_in
  private String lunchPunchOut; // lunch_punch_out
  private String punchIn; // punch_in (string format)
  private String punchOut; // punch_out (string format)

  private String workType;

  // Image path for work from field attendance
  private String imagePath;

  // New fields for EasyTimePro integration
  private String attendanceSource; // "BIOMETRIC", "MANUAL"
  private String attendanceType; // "OFFICE", "WORK_FROM_FIELD"
  private String fieldLocation; // Location for work from field
  private String workDescription; // Description of work done

  // Enhanced biometric integration fields
  private String biometricDeviceId; // Device serial number
  private String biometricUserId; // User ID on biometric device
  private String verifyType; // "fingerprint", "face", "palm"
  private String deviceSerial; // Terminal serial number
  private String punchSource; // "BIOMETRIC", "MANUAL", "API"

  @Column(columnDefinition = "TEXT")
  private String rawData; // Raw data from biometric device

  @ManyToOne
  @JoinColumn(name = "employee_id")
  @JsonBackReference
  private Employee employee;

  private static final List<String> REQUIRES_REASON = List.of("absent", "paidleave");

  @PrePersist
  @PreUpdate
  private void preSave() {
    calculateDurations();
  }

  public void calculateDurations() {
    if (punchInTime != null && punchOutTime != null) {
      LocalTime in = punchInTime.truncatedTo(ChronoUnit.MINUTES);
      LocalTime out = punchOutTime.truncatedTo(ChronoUnit.MINUTES);

      Duration total = Duration.between(in, out);
      if (total.isNegative())
        total = Duration.ZERO;

      Duration lunch = Duration.ZERO;
      if (lunchInTime != null && lunchOutTime != null) {
        LocalTime lin = lunchInTime.truncatedTo(ChronoUnit.MINUTES);
        LocalTime lout = lunchOutTime.truncatedTo(ChronoUnit.MINUTES);
        if (lout.isBefore(lin)) {
          LocalTime tmp = lin;
          lin = lout;
          lout = tmp;
        }
        lunch = Duration.between(lin, lout);
        if (lunch.isNegative())
          lunch = Duration.ZERO;
      }

      this.breakDuration = lunch.toHours() + "h " + lunch.toMinutesPart() + "m";
      Duration actual = total.minus(lunch);
      if (actual.isNegative())
        actual = Duration.ZERO;
      this.workingHours = actual.toHours() + "h " + actual.toMinutesPart() + "m";
    } else {
      this.workingHours = "0h 0m";
      this.breakDuration = "0h 0m";
    }
  }

  private boolean requiresReason(String status) {
    if (status == null)
      return false;
    String normalized = status.trim().toLowerCase().replaceAll("[ _]", "");
    return REQUIRES_REASON.contains(normalized);
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getWorkingHours() {
    return workingHours;
  }

  public void setWorkingHours(String workingHours) {
    this.workingHours = workingHours;
  }

  public String getBreakDuration() {
    return breakDuration;
  }

  public void setBreakDuration(String breakDuration) {
    this.breakDuration = breakDuration;
  }

  public LocalTime getPunchInTime() {
    return punchInTime;
  }

  public void setPunchInTime(LocalTime punchInTime) {
    this.punchInTime = punchInTime != null ? punchInTime.truncatedTo(ChronoUnit.MINUTES) : null;
  }

  public LocalTime getPunchOutTime() {
    return punchOutTime;
  }

  public void setPunchOutTime(LocalTime punchOutTime) {
    this.punchOutTime = punchOutTime != null ? punchOutTime.truncatedTo(ChronoUnit.MINUTES) : null;
  }

  public LocalTime getLunchInTime() {
    return lunchInTime;
  }

  public void setLunchInTime(LocalTime lunchInTime) {
    this.lunchInTime = lunchInTime != null ? lunchInTime.truncatedTo(ChronoUnit.MINUTES) : null;
  }

  public LocalTime getLunchOutTime() {
    return lunchOutTime;
  }

  public void setLunchOutTime(LocalTime lunchOutTime) {
    this.lunchOutTime = lunchOutTime != null ? lunchOutTime.truncatedTo(ChronoUnit.MINUTES) : null;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  /**
   * @return String return the workType
   */
  public String getWorkType() {
    return workType;
  }

  /**
   * @param workType the workType to set
   */
  public void setWorkType(String workType) {
    this.workType = workType;
  }

  /**
   * @return String return the imagePath
   */
  public String getImagePath() {
    return imagePath;
  }

  /**
   * @param imagePath the imagePath to set
   */
  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  /**
   * @return String return the attendanceSource
   */
  public String getAttendanceSource() {
    return attendanceSource;
  }

  /**
   * @param attendanceSource the attendanceSource to set
   */
  public void setAttendanceSource(String attendanceSource) {
    this.attendanceSource = attendanceSource;
  }

  /**
   * @return String return the attendanceType
   */
  public String getAttendanceType() {
    return attendanceType;
  }

  /**
   * @param attendanceType the attendanceType to set
   */
  public void setAttendanceType(String attendanceType) {
    this.attendanceType = attendanceType;
  }

  /**
   * @return String return the fieldLocation
   */
  public String getFieldLocation() {
    return fieldLocation;
  }

  /**
   * @param fieldLocation the fieldLocation to set
   */
  public void setFieldLocation(String fieldLocation) {
    this.fieldLocation = fieldLocation;
  }

  /**
   * @return String return the workDescription
   */
  public String getWorkDescription() {
    return workDescription;
  }

  /**
   * @param workDescription the workDescription to set
   */
  public void setWorkDescription(String workDescription) {
    this.workDescription = workDescription;
  }

  // Getters and setters for biometric fields
  public String getBiometricDeviceId() {
    return biometricDeviceId;
  }

  public void setBiometricDeviceId(String biometricDeviceId) {
    this.biometricDeviceId = biometricDeviceId;
  }

  public String getBiometricUserId() {
    return biometricUserId;
  }

  public void setBiometricUserId(String biometricUserId) {
    this.biometricUserId = biometricUserId;
  }

  public String getVerifyType() {
    return verifyType;
  }

  public void setVerifyType(String verifyType) {
    this.verifyType = verifyType;
  }

  public String getDeviceSerial() {
    return deviceSerial;
  }

  public void setDeviceSerial(String deviceSerial) {
    this.deviceSerial = deviceSerial;
  }

  public String getPunchSource() {
    return punchSource;
  }

  public void setPunchSource(String punchSource) {
    this.punchSource = punchSource;
  }

  public String getRawData() {
    return rawData;
  }

  public void setRawData(String rawData) {
    this.rawData = rawData;
  }

  // Getters and setters for additional fields
  public String getLunchPunchIn() {
    return lunchPunchIn;
  }

  public void setLunchPunchIn(String lunchPunchIn) {
    this.lunchPunchIn = lunchPunchIn;
  }

  public String getLunchPunchOut() {
    return lunchPunchOut;
  }

  public void setLunchPunchOut(String lunchPunchOut) {
    this.lunchPunchOut = lunchPunchOut;
  }

  public String getPunchIn() {
    return punchIn;
  }

  public void setPunchIn(String punchIn) {
    this.punchIn = punchIn;
  }

  public String getPunchOut() {
    return punchOut;
  }

  public void setPunchOut(String punchOut) {
    this.punchOut = punchOut;
  }

}
