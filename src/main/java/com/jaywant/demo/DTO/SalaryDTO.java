package com.jaywant.demo.DTO;

public class SalaryDTO {

	// Basic identifiers and designation info.
	private String uid; // e.g. employee’s phone or a special ID
	private String designation; // e.g. employee’s jobRole or a custom field
	private String accountHolderName; // e.g. “Vishal Nager”

	// Basic salary info.
	private double salary; // (We won't use this for logic now; kept for reference if needed)
	private int workingDays; // e.g., 30/31 in the month
	private double payableDays; // if you still want to track partial days
	private double perDaySalary;
	private double totalPayout; // not used in final logic, but kept if needed
	private double netPayable;

	// Attendance breakdown.
	private int leaveTaken;
	private int holiday;
	private int leaveAllowed;
	private int weekoff;
	private int halfDay;

	// Employee personal/bank details.
	private String firstName;
	private String lastName;
	private String joiningDate; // Corrected from "joiningDatte"
	private String email;
	private String bankName;
	private String bankAccountNo;
	private String branchName;
	private String ifscCode;
	private String jobRole;
	private String aadhar;
	private String department;

	// Salary structure details.
	private double basic; // 50% of monthlyCtc
	private double hra; // 10% of basic
	private double daAllowance; // DA = 53% of basic
	private double specialAllowance; // 37% of basic

	// Additional allowances
	private double transportAllowance;
	private double medicalAllowance;
	private double foodAllowance;

	// Other salary details.
	private double professionalTax; // 2% of monthlyCtc
	private double tds; // 0 or NIL
	private double pf; // Provident Fund
	private double esi; // ESI
	private double advance; // "present-days deduction"
	private double totalDeductions;
	private String additionalPerks;
	private double bonus;
	private double totalAllowance; // HRA + DA + Special
	private double grossSalary; // basic + totalAllowance
	private String amountInWords;

	// Company and subadmin details
	private String companyName;
	private String companyAddress;
	private String companyGstNo;
	private String companyCinNo;
	private String companyUrl;
	private String signatureImage;
	private String stampImage;
	private String companyLogo;

	// =========================
	// Getters and Setters
	// =========================

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

	public String getAccountHolderName() {
		return accountHolderName;
	}

	public void setAccountHolderName(String accountHolderName) {
		this.accountHolderName = accountHolderName;
	}

	public double getSalary() {
		return salary;
	}

	public void setSalary(double salary) {
		this.salary = salary;
	}

	public int getWorkingDays() {
		return workingDays;
	}

	public void setWorkingDays(int workingDays) {
		this.workingDays = workingDays;
	}

	public double getPayableDays() {
		return payableDays;
	}

	public void setPayableDays(double payableDays) {
		this.payableDays = payableDays;
	}

	public double getPerDaySalary() {
		return perDaySalary;
	}

	public void setPerDaySalary(double perDaySalary) {
		this.perDaySalary = perDaySalary;
	}

	public double getTotalPayout() {
		return totalPayout;
	}

	public void setTotalPayout(double totalPayout) {
		this.totalPayout = totalPayout;
	}

	public double getNetPayable() {
		return netPayable;
	}

	public void setNetPayable(double netPayable) {
		this.netPayable = netPayable;
	}

	public int getLeaveTaken() {
		return leaveTaken;
	}

	public void setLeaveTaken(int leaveTaken) {
		this.leaveTaken = leaveTaken;
	}

	public int getHoliday() {
		return holiday;
	}

	public void setHoliday(int holiday) {
		this.holiday = holiday;
	}

	public int getLeaveAllowed() {
		return leaveAllowed;
	}

	public void setLeaveAllowed(int leaveAllowed) {
		this.leaveAllowed = leaveAllowed;
	}

	public int getWeekoff() {
		return weekoff;
	}

	public void setWeekoff(int weekoff) {
		this.weekoff = weekoff;
	}

	public int getHalfDay() {
		return halfDay;
	}

	public void setHalfDay(int halfDay) {
		this.halfDay = halfDay;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getJoiningDate() {
		return joiningDate;
	}

	public void setJoiningDate(String joiningDate) {
		this.joiningDate = joiningDate;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getIfscCode() {
		return ifscCode;
	}

	public void setIfscCode(String ifscCode) {
		this.ifscCode = ifscCode;
	}

	public String getJobRole() {
		return jobRole;
	}

	public void setJobRole(String jobRole) {
		this.jobRole = jobRole;
	}

	public String getAadhar() {
		return aadhar;
	}

	public void setAadhar(String aadhar) {
		this.aadhar = aadhar;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public double getBasic() {
		return basic;
	}

	public void setBasic(double basic) {
		this.basic = basic;
	}

	public double getHra() {
		return hra;
	}

	public void setHra(double hra) {
		this.hra = hra;
	}

	public double getDaAllowance() {
		return daAllowance;
	}

	public void setDaAllowance(double daAllowance) {
		this.daAllowance = daAllowance;
	}

	public double getSpecialAllowance() {
		return specialAllowance;
	}

	public void setSpecialAllowance(double specialAllowance) {
		this.specialAllowance = specialAllowance;
	}

	public double getProfessionalTax() {
		return professionalTax;
	}

	public void setProfessionalTax(double professionalTax) {
		this.professionalTax = professionalTax;
	}

	public double getTds() {
		return tds;
	}

	public void setTds(double tds) {
		this.tds = tds;
	}

	public double getAdvance() {
		return advance;
	}

	public void setAdvance(double advance) {
		this.advance = advance;
	}

	public double getTotalDeductions() {
		return totalDeductions;
	}

	public void setTotalDeductions(double totalDeductions) {
		this.totalDeductions = totalDeductions;
	}

	public String getAdditionalPerks() {
		return additionalPerks;
	}

	public void setAdditionalPerks(String additionalPerks) {
		this.additionalPerks = additionalPerks;
	}

	public double getBonus() {
		return bonus;
	}

	public void setBonus(double bonus) {
		this.bonus = bonus;
	}

	public double getTotalAllowance() {
		return totalAllowance;
	}

	public void setTotalAllowance(double totalAllowance) {
		this.totalAllowance = totalAllowance;
	}

	public double getGrossSalary() {
		return grossSalary;
	}

	public void setGrossSalary(double grossSalary) {
		this.grossSalary = grossSalary;
	}

	public String getAmountInWords() {
		return amountInWords;
	}

	public void setAmountInWords(String amountInWords) {
		this.amountInWords = amountInWords;
	}

	// New getters and setters for additional allowances
	public double getTransportAllowance() {
		return transportAllowance;
	}

	public void setTransportAllowance(double transportAllowance) {
		this.transportAllowance = transportAllowance;
	}

	public double getMedicalAllowance() {
		return medicalAllowance;
	}

	public void setMedicalAllowance(double medicalAllowance) {
		this.medicalAllowance = medicalAllowance;
	}

	public double getFoodAllowance() {
		return foodAllowance;
	}

	public void setFoodAllowance(double foodAllowance) {
		this.foodAllowance = foodAllowance;
	}

	public double getPf() {
		return pf;
	}

	public void setPf(double pf) {
		this.pf = pf;
	}

	public double getEsi() {
		return esi;
	}

	public void setEsi(double esi) {
		this.esi = esi;
	}

	// Company and subadmin details getters and setters
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getCompanyAddress() {
		return companyAddress;
	}

	public void setCompanyAddress(String companyAddress) {
		this.companyAddress = companyAddress;
	}

	public String getCompanyGstNo() {
		return companyGstNo;
	}

	public void setCompanyGstNo(String companyGstNo) {
		this.companyGstNo = companyGstNo;
	}

	public String getCompanyCinNo() {
		return companyCinNo;
	}

	public void setCompanyCinNo(String companyCinNo) {
		this.companyCinNo = companyCinNo;
	}

	public String getCompanyUrl() {
		return companyUrl;
	}

	public void setCompanyUrl(String companyUrl) {
		this.companyUrl = companyUrl;
	}

	public String getSignatureImage() {
		return signatureImage;
	}

	public void setSignatureImage(String signatureImage) {
		this.signatureImage = signatureImage;
	}

	public String getStampImage() {
		return stampImage;
	}

	public void setStampImage(String stampImage) {
		this.stampImage = stampImage;
	}

	public String getCompanyLogo() {
		return companyLogo;
	}

	public void setCompanyLogo(String companyLogo) {
		this.companyLogo = companyLogo;
	}
}
