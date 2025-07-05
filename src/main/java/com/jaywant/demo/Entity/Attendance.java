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

  private String workType;

  // Image path for work from field attendance
  private String imagePath;

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

}
