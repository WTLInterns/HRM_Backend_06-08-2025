package com.jaywant.demo.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Expenses {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Integer expenseId;

  @Column(nullable = false)
  private String date;

  @Column(nullable = false)
  private String billNo;

  @Column(nullable = false)
  private String amount;

  @Column(length = 500)
  private String reason;

  @Column(nullable = false)
  private String transactionId;

  /**
   * Stores only the filename of the uploaded bill image.
   */
  @Column
  private String billImage;

  @ManyToOne
  @JoinColumn(name = "subadmin_id")
  // @JsonBackReference
  @JsonIgnoreProperties({ "expenses" })
  private Subadmin subadmin;

  // ─── Getters & Setters ─────────────────────────────────────────────────────

  public Integer getExpenseId() {
    return expenseId;
  }

  public void setExpenseId(Integer expenseId) {
    this.expenseId = expenseId;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getBillNo() {
    return billNo;
  }

  public void setBillNo(String billNo) {
    this.billNo = billNo;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getBillImage() {
    return billImage;
  }

  public void setBillImage(String billImage) {
    this.billImage = billImage;
  }

  public Subadmin getSubadmin() {
    return subadmin;
  }

  public void setSubadmin(Subadmin subadmin) {
    this.subadmin = subadmin;
  }

}
