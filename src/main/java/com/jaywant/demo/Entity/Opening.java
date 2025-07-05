package com.jaywant.demo.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "openings")
public class Opening {

  @Id
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.AUTO)
  private int id;
  private String role;
  private String location;
  private String siteMode;
  private double positions;
  private String exprience;
  private String description;
  private String workType;

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

  


  // @ManyToOne
  // @JoinColumn(name = "employee_id")
  // private Employee employee;

  @ManyToOne
  @JoinColumn(name = "subadmin_id")
  private Subadmin subadmin;

  /**
   * @return int return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * @return String return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @param role the role to set
   */
  public void setRole(String role) {
    this.role = role;
  }

  /**
   * @return String return the location
   */
  public String getLocation() {
    return location;
  }

  /**
   * @param location the location to set
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * @return String return the siteMode
   */
  public String getSiteMode() {
    return siteMode;
  }

  /**
   * @param siteMode the siteMode to set
   */
  public void setSiteMode(String siteMode) {
    this.siteMode = siteMode;
  }

  /**
   * @return double return the positions
   */
  public double getPositions() {
    return positions;
  }

  /**
   * @param positions the positions to set
   */
  public void setPositions(double positions) {
    this.positions = positions;
  }

  /**
   * @return String return the exprience
   */
  public String getExprience() {
    return exprience;
  }

  /**
   * @param exprience the exprience to set
   */
  public void setExprience(String exprience) {
    this.exprience = exprience;
  }

  /**
   * @return String return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

 

  /**
   * @return Subadmin return the subadmin
   */
  public Subadmin getSubadmin() {
    return subadmin;
  }

  /**
   * @param subadmin the subadmin to set
   */
  public void setSubadmin(Subadmin subadmin) {
    this.subadmin = subadmin;
  }

}
